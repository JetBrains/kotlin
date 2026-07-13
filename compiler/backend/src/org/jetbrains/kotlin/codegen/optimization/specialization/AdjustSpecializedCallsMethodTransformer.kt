/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.specialization

import org.jetbrains.kotlin.codegen.optimization.common.FastMethodAnalyzer
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.codegen.util.inlinecodegen.LightIrType
import org.jetbrains.kotlin.codegen.util.inlinecodegen.LightIrTypeArguments
import org.jetbrains.kotlin.codegen.util.inlinecodegen.SpecTypeParametersUsages
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.InsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame

/**
 * This method transformer adjusts the call-sites of specialized functions. In particular, it does the following:
 * - Replaces boxed types with unboxed representations in indy's signature.
 * - Unboxes arguments of specialized calls.
 * - Boxes results of specialized calls.
 * - Removes <jvm-specialized-argument-marker> markers.
 */
class AdjustSpecializedCallsMethodTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, node: MethodNode) {
        val interpreter = AdjustSpecializedCallsInterpreter()
        val analyzer = FastMethodAnalyzer<BasicValue>(
            internalClassName, node, interpreter, pruneExceptionEdges = false
        ) { nLocals, nStack -> Frame(nLocals, nStack) }
        analyzer.analyze()

        for (specCall in interpreter.specializedCalls.values) {
            adjustSpecializedCall(node.instructions, specCall)
        }

        for ([i, specInterfaceCall] in interpreter.specializedInterfaceCalls.values.withIndex()) {
            val receiverTypeParameterUsage = specInterfaceCall.receiverArg.insn.name.substring("specializedReceiverMarker".length)
            for (arg in specInterfaceCall.arguments) {
                val prefix = "specializedMethodCallArgumentMarker"
                val defaultPrefix = "$prefix-1$"
                require(arg.insn.name.startsWith(defaultPrefix))
                arg.insn.name = "${prefix}$i$${arg.insn.name.substring(defaultPrefix.length)}"
            }
            node.instructions.insertBefore(
                specInterfaceCall.insn, MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "kotlin/jvm/internal/Intrinsics",
                    "specializedInterfaceCall$i$$receiverTypeParameterUsage",
                    "()V",
                    false,
                )
            )
        }

        node.instructions.removeAll { it is MethodInsnNode && it.isSpecializedArgumentMarker }
    }
}

private fun adjustSpecializedCall(instructions: InsnList, specCall: SpecializedCall) {
    val specTypeParametersUsages = SpecTypeParametersUsages.decode(specCall.insn.bsmArgs[2] as String)
    val specializedTypeArguments = LightIrTypeArguments.decode(specCall.insn.bsmArgs[3] as String)
    var newReturnType = Type.getReturnType(specCall.insn.desc)
    val newDescArgs = Type.getArgumentTypes(specCall.insn.desc)

    for ([argI, typeParameterUsage] in specTypeParametersUsages.parameterGenericIndices) {
        val typeParameter = typeParameterUsage.adjustType(specializedTypeArguments) ?: continue
        when (val classifier = typeParameter.classifier) {
            is LightIrType.Classifier.Clazz -> {
                val abi = typeParameter.specializedAbi() ?: continue
                newDescArgs[argI] = Type.getType(abi.reprDesc)
                val argValue = specCall.args[argI]
                if (argValue is SpecializedArgumentValue) {
                    val placeholder = InsnNode(Opcodes.NOP)
                    instructions.insert(argValue.insn, placeholder)
                    abi.genUnbox(instructions, placeholder)
                }
            }
            is LightIrType.Classifier.TypeParameter -> {
                if (!classifier.specialized) continue
                val usage = SpecTypeParametersUsages.Usage(classifier.index, typeParameter.nullable)
                val argValue = specCall.args[argI]
                if (argValue is SpecializedArgumentValue) {
                    instructions.insert(
                        argValue.insn, MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "kotlin/jvm/internal/Intrinsics",
                            "unboxMarker${usage.encode()}",
                            "(Ljava/lang/Object;)Lkotlin/jvm/internal/SpecUnboxedDecoy${usage.encode()};",
                            false,
                        )
                    )
                }
            }
        }
    }

    specTypeParametersUsages.returnType?.adjustType(specializedTypeArguments)?.let { typeParameter ->
        when (val classifier = typeParameter.classifier) {
            is LightIrType.Classifier.Clazz -> {
                typeParameter.specializedAbi()?.also { abi ->
                    newReturnType = Type.getType(abi.reprDesc)
                    val placeholder = InsnNode(Opcodes.NOP)
                    instructions.insert(specCall.insn, placeholder)
                    abi.genBox(instructions, placeholder)
                }
            }
            is LightIrType.Classifier.TypeParameter -> {
                if (classifier.specialized) {
                    val usage = SpecTypeParametersUsages.Usage(classifier.index, typeParameter.nullable)
                    instructions.insert(
                        specCall.insn, MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "kotlin/jvm/internal/Intrinsics",
                            "boxMarker${usage.encode()}",
                            "(Ljava/lang/Object;)Lkotlin/jvm/internal/SpecBoxedDecoy${usage.encode()};",
                            false,
                        )
                    )
                }
            }
        }
    }

    specCall.insn.desc = Type.getMethodType(newReturnType, *newDescArgs).descriptor
}
