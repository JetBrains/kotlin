/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.util.inlinecodegen

import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.LabelNode
import org.jetbrains.org.objectweb.asm.tree.LineNumberNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

enum class ReifiedOperationKind {
    NEW_ARRAY, AS, SAFE_AS, IS, JAVA_CLASS, ENUM_REIFIED, TYPE_OF, CATCH;

    val id: Int get() = ordinal
}

data class ReificationArgument(
    val parameterName: String, val nullable: Boolean, val arrayDepth: Int
) {
    fun asString(): String =
        "[".repeat(arrayDepth) + parameterName + (if (nullable) "?" else "")

    fun combine(replacement: ReificationArgument): ReificationArgument =
        ReificationArgument(
            replacement.parameterName,
            this.nullable || (replacement.nullable && this.arrayDepth == 0),
            this.arrayDepth + replacement.arrayDepth
        )
}

fun processCatch(
    markerInsn: MethodInsnNode,
    node: MethodNode,
    asmType: Type,
): Boolean {
    var labelInsn = markerInsn.findPreviousOrNull { it is LabelNode } as LabelNode?
        ?: error("cannot locate label of catch block handler")

    var catchBlock = node.tryCatchBlocks.find { it.handler == labelInsn && it.type != null }

    // there might be a LABEL and LINE_NUMBER before the actual start of the handler
    if (catchBlock == null && labelInsn.next is LineNumberNode) {
        labelInsn = labelInsn.findPreviousOrNull { it is LabelNode } as LabelNode?
            ?: error("cannot locate label of catch block handler before line number")

        catchBlock = node.tryCatchBlocks.find { it.handler == labelInsn && it.type != null } ?: return false
    }

    if (catchBlock == null) error("cannot identify catch block")

    // null-check is not required for catch
    catchBlock.type = asmType.internalName

    return true
}
