/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.specialization

import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.codegen.util.inlinecodegen.JvmSpecializeMetadataValue
import org.jetbrains.kotlin.codegen.util.inlinecodegen.extractJvmSpecializeMetadataValue
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.Analyzer
import java.util.TreeSet

/**
 * This method transformer does two things:
 * - Tracks loads and stores of specialized values and inserts markers for them.
 * - Tracks which local variable slots are ever used by specialized values and stores this information in the metadata.
 */
class SpecializationTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val metadataValue = methodNode.extractJvmSpecializeMetadataValue() ?: return

        val localToArgumentIndex = buildMap {
            var local = 0
            var idx = 0
            if (methodNode.access and Opcodes.ACC_STATIC == 0) {
                put(local++, idx++)
            }
            for (argumentType in Type.getArgumentTypes(methodNode.desc)) {
                put(local, idx++)
                local += argumentType.size
            }
        }

        val interpreter = SpecializationInterpreter(localToArgumentIndex, metadataValue)
        val analyzer = Analyzer(interpreter)
        val frames = analyzer.analyze(internalClassName, methodNode)

        for ([insn, genericUsage] in interpreter.specializedLoadStore) {
            methodNode.instructions.insertBefore(
                insn, MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "kotlin/jvm/internal/Intrinsics",
                    "specializedTypeMarker${genericUsage.encode()}",
                    "()V",
                    false
                )
            )
        }

        val specializedSlotsMap = HashMap<Int, TreeSet<Int>>() // genericIndex -> slots
        for (frame in frames) {
            val frame = frame ?: continue
            for (slot in 0 until frame.locals) {
                frame.getLocal(slot)?.genericUsage?.let { genericUsage ->
                    if (!genericUsage.nullable) {
                        specializedSlotsMap.getOrPut(genericUsage.genericIndex, ::TreeSet).add(slot)
                    }
                }
            }
        }
        val specializedSlots = buildList {
            for ([genericIndex, slots] in specializedSlotsMap) {
                add(genericIndex)
                add(slots.size)
                for (slot in slots) {
                    add(slot)
                }
            }
        }
        methodNode.invisibleAnnotations.removeAll { it.desc == JvmSpecializeMetadataValue.ANNOTATION_DESCRIPTOR_NAME }
        methodNode.invisibleAnnotations.add(metadataValue.copy(specializedSlots = specializedSlots).toAnnotationNode())

        methodNode.instructions.forEach { insn ->
            if (insn is MethodInsnNode &&
                insn.owner == "kotlin/jvm/internal/Intrinsics" &&
                (insn.name.startsWith("boxMarker") ||
                        insn.name.startsWith("unboxMarker") ||
                        insn.name.startsWith("coerce2NullableMarker") ||
                        insn.name.startsWith("coerce2NonNullableMarker"))
            ) {
                val argType = Type.getArgumentTypes(insn.desc)[0]
                insn.desc = Type.getMethodDescriptor(argType, argType)
            }
        }
    }
}
