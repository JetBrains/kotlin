/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.impl

import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.incremental.KotlinClassInfo.ExtraInfo
import org.jetbrains.kotlin.incremental.impl.ClassNodeSnapshotter.snapshotClassExcludingMembers
import org.jetbrains.kotlin.incremental.impl.ClassNodeSnapshotter.snapshotMethod
import org.jetbrains.kotlin.incremental.impl.ClassNodeSnapshotter.sortClassMembers
import org.jetbrains.kotlin.incremental.storage.*
import org.jetbrains.kotlin.inline.InlineFunctionOrAccessor
import org.jetbrains.kotlin.inline.inlineFunctionsAndAccessors
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.tree.ClassNode


open class ExtraClassInfoGenerator() {
    protected open fun makeClassVisitor(classNode: ClassNode): ClassVisitor {
        return classNode
    }

    protected open fun calculateInlineMethodHash(methodSignature: JvmMemberSignature.Method, ownMethodHash: Long): Long {
        return ownMethodHash
    }

    fun getExtraInfo(classHeader: KotlinClassHeader, classContents: ByteArray): ExtraInfo {
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
            cv = makeClassVisitor(classNode),
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
            var methodHash = snapshotMethod(methodNode, classNode.version)
            inlineFunctionsAndAccessors[methodSignature]!! to calculateInlineMethodHash(methodSignature, methodHash)
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
