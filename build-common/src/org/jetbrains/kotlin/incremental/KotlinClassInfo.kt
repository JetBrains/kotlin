/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.ClassNodeSnapshotter.snapshotClassExcludingMembers
import org.jetbrains.kotlin.incremental.ClassNodeSnapshotter.snapshotField
import org.jetbrains.kotlin.incremental.ClassNodeSnapshotter.snapshotMethod
import org.jetbrains.kotlin.incremental.ClassNodeSnapshotter.sortClassMembers
import org.jetbrains.kotlin.incremental.KotlinClassInfo.ExtraInfo
import org.jetbrains.kotlin.incremental.storage.ProtoMapValue
import org.jetbrains.kotlin.inline.InlineFunctionOrAccessor
import org.jetbrains.kotlin.inline.inlineFunctionsAndAccessors
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.jvm.deserialization.BitEncoding
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

/**
 * Minimal information about a Kotlin class to compute recompilation-triggering changes during an incremental run of the `KotlinCompile`
 * task (see [IncrementalJvmCache.saveClassToCache]).
 *
 * It's important that this class contain only the minimal required information, as it will be part of the classpath snapshot of the
 * `KotlinCompile` task and the task needs to support compile avoidance. For example, this class should contain public method signatures,
 * and should not contain private method signatures, or method implementations.
 */
class KotlinClassInfo(
    val classId: ClassId,
    val classKind: KotlinClassHeader.Kind,
    val classHeaderData: Array<String>, // Can be empty
    val classHeaderStrings: Array<String>, // Can be empty
    val multifileClassName: String?, // Not null iff classKind == KotlinClassHeader.Kind.MULTIFILE_CLASS_PART
    val extraInfo: ExtraInfo
) {

    /** Extra information about a Kotlin class that is not captured in the Kotlin class metadata. */
    class ExtraInfo(

        /**
         * Snapshot of the class excluding its fields and methods and Kotlin metadata (iff classKind == [KotlinClassHeader.Kind.CLASS]).
         *
         * For example, the class's annotations which are currently not captured by Kotlin metadata (KT-57919) will be captured here.
         *
         * Note: It also excludes Kotlin metadata as [ExtraInfo] should only contain info not yet captured in Kotlin metadata.
         */
        val classSnapshotExcludingMembers: Long?,

        /** Snapshots of the class's constants (including their values). The map's keys are the constants' names. */
        val constantSnapshots: Map<String, Long>,

        /** Snapshots of the class's inline functions and property accessors (including their implementation). */
        val inlineFunctionOrAccessorSnapshots: Map<InlineFunctionOrAccessor, Long>
    )

    val className: JvmClassName by lazy { JvmClassName.byClassId(classId) }

    val protoMapValue: ProtoMapValue by lazy {
        ProtoMapValue(
            isPackageFacade = classKind != KotlinClassHeader.Kind.CLASS,
            BitEncoding.decodeBytes(classHeaderData),
            classHeaderStrings
        )
    }

    /**
     * The [ProtoData] of this class.
     *
     * NOTE: The caller needs to ensure `classKind != KotlinClassHeader.Kind.MULTIFILE_CLASS` first, as the compiler doesn't write proto
     * data to [KotlinClassHeader.Kind.MULTIFILE_CLASS] classes.
     */
    val protoData: ProtoData by lazy {
        check(classKind != KotlinClassHeader.Kind.MULTIFILE_CLASS) {
            "Proto data is not available for KotlinClassHeader.Kind.MULTIFILE_CLASS: $classId"
        }
        protoMapValue.toProtoData(classId.packageFqName)
    }

    /** Name of the companion object of this class (default is "Companion") iff this class HAS a companion object, or null otherwise. */
    val companionObject: ClassId? by lazy {
        if (classKind == KotlinClassHeader.Kind.CLASS) {
            (protoData as ClassProtoData).getCompanionObjectName()?.let {
                classId.createNestedClassId(Name.identifier(it))
            }
        } else null
    }

    /** List of constants defined in this class iff this class IS a companion object, or null otherwise. The list could be empty. */
    val constantsInCompanionObject: List<String>? by lazy {
        if (classKind == KotlinClassHeader.Kind.CLASS) {
            val classProtoData = protoData as ClassProtoData
            if (classProtoData.proto.isCompanionObject) {
                classProtoData.getConstants()
            } else null
        } else null
    }

    companion object {

        fun createFrom(kotlinClass: LocalFileKotlinClass): KotlinClassInfo {
            return createFrom(kotlinClass.classId, kotlinClass.classHeader, kotlinClass.fileContents)
        }

        fun createFrom(classId: ClassId, classHeader: KotlinClassHeader, classContents: ByteArray): KotlinClassInfo {
            return KotlinClassInfo(
                classId,
                classHeader.kind,
                classHeader.data ?: classHeader.incompatibleData ?: emptyArray(),
                classHeader.strings ?: emptyArray(),
                classHeader.multifileClassName,
                extraInfo = getExtraInfo(classHeader, classContents)
            )
        }
    }
}

private fun getExtraInfo(classHeader: KotlinClassHeader, classContents: ByteArray): ExtraInfo {
    // Get the list of (non-private) inline functions and accessors from Kotlin class metadata, then find and snapshot them in the bytecode.
    // Note:
    //   - Some of them may not be found in the bytecode. Specifically, internal/private inline functions/accessors may be removed from the
    // bytecode if code shrinker is used. For example, `kotlin-reflect-1.7.20.jar` contains `/kotlin/reflect/jvm/internal/UtilKt.class` in
    // which the internal inline function `reflectionCall` appears in the Kotlin class metadata (also in the source file), but not in the
    // bytecode. When that happens, we will ignore those methods. It is safe to ignore because the methods are not declared in the bytecode
    // and therefore can't be referenced.
    //   - Look for private methods as well because a *public* inline function/accessor may have a *private* corresponding method in the
    // bytecode (see `InlineOnlyKt.isInlineOnlyPrivateInBytecode`).
    val inlineFunctionsAndAccessors: List<InlineFunctionOrAccessor> = inlineFunctionsAndAccessors(classHeader, excludePrivateMembers = true)
    val inlineFunctionOrAccessorSignatures: Map<JvmMemberSignature.Method, InlineFunctionOrAccessor> =
        inlineFunctionsAndAccessors.associateBy { it.jvmMethodSignature }

    val parsingOptions = if (inlineFunctionsAndAccessors.isNotEmpty()) {
        // Do not pass (SKIP_CODE, SKIP_DEBUG) as method bodies and debug info (e.g., line numbers) are important for inline
        // functions/accessors
        0
    } else {
        // Pass (SKIP_CODE, SKIP_DEBUG) to improve performance as method bodies and debug info are not important when we're not analyzing
        // inline functions/accessors
        ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG
    }

    // Load class contents into a `ClassNode`.
    // Note that we'll only load methods that are inline functions/accessors (including private methods -- see comment at the top of
    // `getExtraInfo`) as we don't need to snapshot the other methods when computing `ExtraInfo`.
    val classNode = ClassNode()
    val classReader = ClassReader(classContents)
    classReader.accept(SkipMethodClassVisitor(classNode) { it !in inlineFunctionOrAccessorSignatures.keys }, parsingOptions)
    sortClassMembers(classNode)

    // Snapshot the class excluding its fields and methods and metadata
    val classSnapshotExcludingMembers = if (classHeader.kind == KotlinClassHeader.Kind.CLASS) {
        // Also exclude Kotlin metadata (see `ExtraInfo.classSnapshotExcludingMembers`'s kdoc)
        snapshotClassExcludingMembers(classNode, alsoExcludeKotlinMetaData = true)
    } else null

    // Snapshot constants
    fun FieldNode.isPrivate() = (access and Opcodes.ACC_PRIVATE) != 0
    fun FieldNode.isStaticFinal() = (access and (Opcodes.ACC_STATIC or Opcodes.ACC_FINAL)) == (Opcodes.ACC_STATIC or Opcodes.ACC_FINAL)

    val constantSnapshots: Map<String, Long> = classNode.fields
        .filter { !it.isPrivate() && it.isStaticFinal() }
        .associate { it.name to snapshotField(it) }

    // Snapshot inline functions and accessors
    fun MethodNode.signature() = JvmMemberSignature.Method(name = name, desc = desc)

    val inlineFunctionOrAccessorSnapshots: Map<InlineFunctionOrAccessor, Long> = classNode.methods
        .associate { methodNode ->
            // `methodNode` must be an inline function/accessor because we loaded only inline functions/accessors into `classNode`
            inlineFunctionOrAccessorSignatures[methodNode.signature()]!! to snapshotMethod(methodNode, classNode.version)
        }

    return ExtraInfo(classSnapshotExcludingMembers, constantSnapshots, inlineFunctionOrAccessorSnapshots)
}

/** [ClassVisitor] which skips visiting methods where `[shouldSkipMethod] == true`. */
private class SkipMethodClassVisitor(
    cv: ClassVisitor,
    private val shouldSkipMethod: (JvmMemberSignature.Method) -> Boolean,
) : ClassVisitor(Opcodes.API_VERSION, cv) {

    override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        return if (shouldSkipMethod(JvmMemberSignature.Method(name, desc))) {
            null
        } else {
            cv.visitMethod(access, name, desc, signature, exceptions)
        }
    }
}

/** Computes the snapshot of a Java class represented by a [ClassNode]. */
object ClassNodeSnapshotter {

    fun snapshotClass(classNode: ClassNode): Long {
        val classWriter = ClassWriter(0)
        classNode.accept(classWriter)
        return classWriter.toByteArray().hashToLong()
    }

    fun snapshotClassExcludingMembers(classNode: ClassNode, alsoExcludeKotlinMetaData: Boolean = false): Long {
        val originalFields = classNode.fields
        val originalMethods = classNode.methods
        val originalVisibleAnnotations = classNode.visibleAnnotations
        classNode.fields = emptyList()
        classNode.methods = emptyList()
        if (alsoExcludeKotlinMetaData) {
            classNode.visibleAnnotations = originalVisibleAnnotations?.filterNot { it.desc == "Lkotlin/Metadata;" }
        }
        return snapshotClass(classNode).also {
            classNode.fields = originalFields
            classNode.methods = originalMethods
            classNode.visibleAnnotations = originalVisibleAnnotations
        }
    }

    fun snapshotField(fieldNode: FieldNode): Long {
        val classNode = emptyClass()
        classNode.fields.add(fieldNode)
        return snapshotClass(classNode)
    }

    fun snapshotMethod(methodNode: MethodNode, classVersion: Int): Long {
        val classNode = emptyClass()
        classNode.version = classVersion // Class version is required for method bodies (see KT-38857)
        classNode.methods.add(methodNode)
        return snapshotClass(classNode)
    }

    /**
     * Sorts fields and methods in the given class.
     *
     * This is useful when we want to ensure a change in the order of the fields and methods doesn't impact the snapshot (i.e., if their
     * order has changed in the `.class` file, it shouldn't require recompilation of the other source files).
     */
    fun sortClassMembers(classNode: ClassNode) {
        classNode.fields.sortWith(compareBy({ it.name }, { it.desc }))
        classNode.methods.sortWith(compareBy({ it.name }, { it.desc }))
    }

    private fun emptyClass() = ClassNode().also {
        // A name is required
        it.name = "EmptyClass"
    }
}

fun ByteArray.hashToLong(): Long {
    // Note: The returned type `Long` is 64-bit, but we currently don't have a good 64-bit hash function.
    // The method below uses `md5` which is 128-bit and converts it to `Long`.
    return md5()
}
