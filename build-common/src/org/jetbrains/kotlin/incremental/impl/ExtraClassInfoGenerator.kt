/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.impl

import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.incremental.ClassProtoData
import org.jetbrains.kotlin.incremental.KotlinClassInfo.ExtraInfo
import org.jetbrains.kotlin.incremental.PackagePartProtoData
import org.jetbrains.kotlin.incremental.ProtoData
import org.jetbrains.kotlin.incremental.impl.ClassNodeSnapshotter.snapshotClassExcludingMembers
import org.jetbrains.kotlin.incremental.impl.ClassNodeSnapshotter.snapshotMethod
import org.jetbrains.kotlin.incremental.storage.*
import org.jetbrains.kotlin.inline.InlineFunctionOrAccessor
import org.jetbrains.kotlin.inline.inlineFunctions
import org.jetbrains.kotlin.inline.inlineFunctionsAndAccessors
import org.jetbrains.kotlin.inline.inlinePropertyAccessors
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.tree.ClassNode


open class ExtraClassInfoGenerator {
    protected open fun makeClassVisitor(): ClassVisitor? = null

    /**
     * @param methodSignature well-typed method signature. doesn't include the containing class' internal name
     * @param inlinedClassPrefix - includes class internal name and method name. example value is "com/bar/OuterClass$InnerClass$calculate"
     * @param ownMethodHash - a basic intuition is that it's based on bytecode and debug info
     */
    protected open fun calculateInlineMethodHash(
        methodSignature: JvmMemberSignature.Method,
        inlinedClassPrefix: String,
        ownMethodHash: Long
    ): Long {
        return ownMethodHash
    }

    fun getExtraInfo(classHeader: KotlinClassHeader, classContents: ByteArray): ExtraInfo {
        return getExtraInfo(classHeader, ClassReader(classContents), inlineFunctionsAndAccessors(classHeader, excludePrivateMembers = true))
    }

    fun getExtraInfo(classHeader: KotlinClassHeader, classReader: ClassReader, classProto: ProtoData?): ExtraInfo {
        return getExtraInfo(classHeader, classNode(classReader), classProto)
    }

    fun getExtraInfo(classHeader: KotlinClassHeader, classNode: ClassNode, classProto: ProtoData?): ExtraInfo {
        val inlineMembers = when (classProto) {
            is ClassProtoData ->
                inlineFunctions(classProto.proto.functionList, classProto.nameResolver, classProto.proto.typeTable, excludePrivateFunctions = true) +
                        inlinePropertyAccessors(classProto.proto.propertyList, classProto.nameResolver, excludePrivateAccessors = true)
            is PackagePartProtoData ->
                inlineFunctions(classProto.proto.functionList, classProto.nameResolver, classProto.proto.typeTable, excludePrivateFunctions = true) +
                        inlinePropertyAccessors(classProto.proto.propertyList, classProto.nameResolver, excludePrivateAccessors = true)
            null -> emptyList()
        }
        return getExtraInfo(classHeader, classNode, inlineMembers)
    }

    /** Allows reusing already discovered non-private inline functions and accessors. */
    fun getExtraInfo(
        classHeader: KotlinClassHeader,
        classReader: ClassReader,
        inlineMembers: List<InlineFunctionOrAccessor>
    ): ExtraInfo {
        return getExtraInfo(classHeader, classNode(classReader), inlineMembers)
    }

    private fun getExtraInfo(
        classHeader: KotlinClassHeader,
        classNode: ClassNode,
        inlineMembers: List<InlineFunctionOrAccessor>
    ): ExtraInfo {
        val inlineFunctionsAndAccessors: Map<JvmMemberSignature.Method, InlineFunctionOrAccessor> =
            inlineMembers.associateBy { it.jvmMethodSignature }

        // Do not filter private bytecode methods: non-private inline members may have private implementations.
        val inlineMethods = classNode.methods
            .filter { JvmMemberSignature.Method(it.name, it.desc) in inlineFunctionsAndAccessors }
            .sortedWith(compareBy({ it.name }, { it.desc }))
        makeClassVisitor()?.let { visitor -> inlineMethods.forEach { it.accept(visitor) } }

        val inlineFunctionOrAccessorSnapshots: Map<InlineFunctionOrAccessor, Long> = inlineMethods.associate { methodNode ->
            // Note:
            //   - Each method in `inlineMethods` is a non-private inline function/accessor.
            //   - Not all inline functions/accessors have a corresponding method in the bytecode (i.e., it's possible that
            //     `inlineMethods.size < inlineFunctionsAndAccessors.size`). Specifically, internal/private inline functions/accessors may
            //     be removed from the bytecode if code shrinker is used. For example, `kotlin-reflect-1.7.20.jar` contains
            //     `/kotlin/reflect/jvm/internal/UtilKt.class` in which the internal inline function `reflectionCall` appears in the Kotlin
            //     class metadata (also in the source file), but not in the bytecode. However, we can safely ignore those
            //     inline functions/accessors because they are not declared in the bytecode and therefore can't be referenced.
            val methodSignature = JvmMemberSignature.Method(name = methodNode.name, desc = methodNode.desc)
            val innerClassPrefix = "${classNode.name}\$${methodNode.name}"
            val methodHash = snapshotMethod(methodNode, classNode.version)
            inlineFunctionsAndAccessors[methodSignature]!! to calculateInlineMethodHash(methodSignature, innerClassPrefix, methodHash)
        }

        val classSnapshotExcludingMembers = if (classHeader.kind == KotlinClassHeader.Kind.CLASS) {
            // Also exclude Kotlin metadata (see `ExtraInfo.classSnapshotExcludingMembers`'s kdoc)
            snapshotClassExcludingMembers(
                classNode,
                alsoExcludeKotlinMetaData = true,
                alsoExcludeDebugInfo = inlineFunctionOrAccessorSnapshots.isEmpty(),
            )
        } else null

        val constantSnapshots: Map<String, Long> = classNode.fields
            .filter { !it.isPrivate() && it.isConstant() }
            .sortedWith(compareBy({ it.name }, { it.desc }))
            .associate { fieldNode ->
                fieldNode.name to ConstantValueExternalizer.toByteArray(fieldNode.value!!).hashToLong()
            }

        return ExtraInfo(classSnapshotExcludingMembers, constantSnapshots, inlineFunctionOrAccessorSnapshots)
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
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
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
