/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.incremental.ClassNodeSnapshotter.snapshotClassExcludingMembers
import org.jetbrains.kotlin.incremental.ClassNodeSnapshotter.snapshotMethod
import org.jetbrains.kotlin.incremental.ClassNodeSnapshotter.sortClassMembers
import org.jetbrains.kotlin.incremental.KotlinClassInfo.ExtraInfo
import org.jetbrains.kotlin.incremental.storage.*
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
         * Snapshot of the class excluding its fields and methods and Kotlin metadata. It is not null iff
         * [classKind] == [KotlinClassHeader.Kind.CLASS].
         *
         * Note: Kotlin metadata is excluded because [ExtraInfo] is meant to contain information that supplements Kotlin metadata. (We have
         * a separate logic for comparing protos constructed from Kotlin metadata. That logic considers only changes in protos/Kotlin
         * metadata that are important for incremental compilation. If we don't exclude Kotlin metadata here, we might report a change in
         * Kotlin metadata even when the change is not important for incremental compilation.)
         *
         * TODO(KT-59292): Consider removing this info once class annotations are included in Kotlin metadata.
         */
        val classSnapshotExcludingMembers: Long?,

        /**
         * Snapshots of the class's non-private constants.
         *
         * Each entry maps a constant's name to the hash of its value.
         */
        val constantSnapshots: Map<String, Long>,

        /**
         * Snapshots of the class's non-private inline functions and property accessors.
         *
         * Each entry maps an inline function or property accessor to the hash of its corresponding method in the bytecode (including the
         * method's body).
         */
        val inlineFunctionOrAccessorSnapshots: Map<InlineFunctionOrAccessor, Long>,
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
    val inlineFunctionsAndAccessors: Map<JvmMemberSignature.Method, InlineFunctionOrAccessor> =
        inlineFunctionsAndAccessors(classHeader, excludePrivateMembers = true).associateBy { it.jvmMethodSignature }

    // 1. Create a ClassNode that will contain only required info
    val classNode = ClassNode()

    // 2. Load the class's contents into the ClassNode, keeping only info that is required to compute `ExtraInfo`:
    //     - Keep only fields that are non-private constants
    //     - Keep only methods that are non-private inline functions/accessors
    //        + Do not filter out private methods because a *non-private* inline function/accessor may have a *private* corresponding method
    //          in the bytecode (see `InlineOnlyKt.isInlineOnlyPrivateInBytecode`)
    //        + Do not filter out method bodies
    val classReader = ClassReader(classContents)
    val selectiveClassVisitor = SelectiveClassVisitor(
        classNode,
        shouldVisitField = { _: JvmMemberSignature.Field, isPrivate: Boolean, isConstant: Boolean ->
            !isPrivate && isConstant
        },
        shouldVisitMethod = { method: JvmMemberSignature.Method, _: Boolean ->
            // Do not filter out private methods (see above comment)
            method in inlineFunctionsAndAccessors.keys
        }
    )
    val parsingOptions = if (inlineFunctionsAndAccessors.isNotEmpty()) {
        // Do not pass (SKIP_CODE, SKIP_DEBUG) as method bodies and debug info (e.g., line numbers) are important for inline
        // functions/accessors
        0
    } else {
        // Pass (SKIP_CODE, SKIP_DEBUG) to improve performance as method bodies and debug info are not important when we're not analyzing
        // inline functions/accessors
        ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG
    }
    classReader.accept(selectiveClassVisitor, parsingOptions)

    // 3. Sort fields and methods as their order is not important
    sortClassMembers(classNode)

    // 4. Snapshot the class
    val classSnapshotExcludingMembers = if (classHeader.kind == KotlinClassHeader.Kind.CLASS) {
        // Also exclude Kotlin metadata (see `ExtraInfo.classSnapshotExcludingMembers`'s kdoc)
        snapshotClassExcludingMembers(classNode, alsoExcludeKotlinMetaData = true)
    } else null

    val constantSnapshots: Map<String, Long> = classNode.fields.associate { fieldNode ->
        // Note: `fieldNode` is a constant because we kept only fields that are (non-private) constants in `classNode`
        fieldNode.name to ConstantValueExternalizer.toByteArray(fieldNode.value!!).hashToLong()
    }

    val inlineFunctionOrAccessorSnapshots: Map<InlineFunctionOrAccessor, Long> = classNode.methods.associate { methodNode ->
        // Note:
        //   - Each of `classNode.methods` (`methodNode`) is an inline function/accessor because we kept only methods that are (non-private)
        //     inline functions/accessors in `classNode`.
        //   - Not all inline functions/accessors have a corresponding method in the bytecode (i.e., it's possible that
        //     `classNode.methods.size < inlineFunctionsAndAccessors.size`). Specifically, internal/private inline functions/accessors may
        //     be removed from the bytecode if code shrinker is used. For example, `kotlin-reflect-1.7.20.jar` contains
        //     `/kotlin/reflect/jvm/internal/UtilKt.class` in which the internal inline function `reflectionCall` appears in the Kotlin
        //     class metadata (also in the source file), but not in the bytecode. However, we can safely ignore those
        //     inline functions/accessors because they are not declared in the bytecode and therefore can't be referenced.
        val methodSignature = JvmMemberSignature.Method(name = methodNode.name, desc = methodNode.desc)
        inlineFunctionsAndAccessors[methodSignature]!! to snapshotMethod(methodNode, classNode.version)
    }

    return ExtraInfo(classSnapshotExcludingMembers, constantSnapshots, inlineFunctionOrAccessorSnapshots)
}

/**
 * [ClassVisitor] which visits only members satisfying the given criteria (`[shouldVisitField] == true` or `[shouldVisitMethod] == true`).
 */
class SelectiveClassVisitor(
    cv: ClassVisitor,
    private val shouldVisitField: (JvmMemberSignature.Field, isPrivate: Boolean, isConstant: Boolean) -> Boolean,
    private val shouldVisitMethod: (JvmMemberSignature.Method, isPrivate: Boolean) -> Boolean,
) : ClassVisitor(Opcodes.API_VERSION, cv) {

    override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
        // Note: A constant's value must be not-null. A static final field with a `null` value at the bytecode declaration is not a constant
        // (whether the value is initialized later in the static initializer or not, it won't be inlined by the compiler).
        val isConstant = access.isStaticFinal() && value != null

        return if (shouldVisitField(JvmMemberSignature.Field(name, desc), access.isPrivate(), isConstant)) {
            cv.visitField(access, name, desc, signature, value)
        } else null
    }

    override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        return if (shouldVisitMethod(JvmMemberSignature.Method(name, desc), access.isPrivate())) {
            cv.visitMethod(access, name, desc, signature, exceptions)
        } else null
    }

    private fun Int.isPrivate() = (this and Opcodes.ACC_PRIVATE) != 0

    private fun Int.isStaticFinal() = (this and (Opcodes.ACC_STATIC or Opcodes.ACC_FINAL)) == (Opcodes.ACC_STATIC or Opcodes.ACC_FINAL)

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
        classNode.version = classVersion // Class version is required when working with methods (without it, ASM may fail -- see KT-38857)
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
        it.name = "SomeClass"
    }
}

/**
 * [DataExternalizer] for the value of a constant.
 *
 * A constant's value must be not-null and must be one of the following types: Integer, Long, Float, Double, String (see the javadoc of
 * [ClassVisitor.visitField]).
 *
 * Side note: The value of a Boolean constant is represented as an Integer (0, 1) value.
 */
private object ConstantValueExternalizer : DataExternalizer<Any> by DelegateDataExternalizer(
    listOf(
        java.lang.Integer::class.java,
        java.lang.Long::class.java,
        java.lang.Float::class.java,
        java.lang.Double::class.java,
        java.lang.String::class.java
    ),
    listOf(IntExternalizer, LongExternalizer, FloatExternalizer, DoubleExternalizer, StringExternalizer)
)

fun ByteArray.hashToLong(): Long {
    // Note: The returned type `Long` is 64-bit, but we currently don't have a good 64-bit hash function.
    // The method below uses `md5` which is 128-bit and converts it to `Long`.
    return md5()
}
