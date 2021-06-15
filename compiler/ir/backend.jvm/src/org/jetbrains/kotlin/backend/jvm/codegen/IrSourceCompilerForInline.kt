/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.jvm.ir.getKtFile
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.hasMangledReturnType
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.incremental.components.LocationInfo
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.module
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.psi.doNotAnalyze
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.SUSPENSION_POINT_INSIDE_MONITOR
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class IrSourceCompilerForInline(
    override val state: GenerationState,
    override val callElement: IrFunctionAccessExpression,
    private val callee: IrFunction,
    internal val codegen: ExpressionCodegen,
    private val data: BlockInfo
) : SourceCompilerForInline {
    override val callElementText: String
        get() = ir2string(callElement)

    override val inlineCallSiteInfo: InlineCallSiteInfo
        get() {
            val root = generateSequence(codegen) { it.inlinedInto }.last()
            return InlineCallSiteInfo(
                root.classCodegen.type.internalName,
                root.signature.asmMethod,
                root.irFunction.isInlineOrInsideInline(),
                root.irFunction.fileParent.getKtFile(),
                callElement.psiElement?.let { CodegenUtil.getLineNumberForElement(it, false) } ?: 0
            )
        }

    override val sourceMapper: SourceMapper
        get() = codegen.smap

    override fun generateLambdaBody(lambdaInfo: ExpressionLambda, reifiedTypeParameters: ReifiedTypeParametersUsages): SMAPAndMethodNode {
        require(lambdaInfo is IrExpressionLambdaImpl)
        for (typeParameter in lambdaInfo.function.typeParameters) {
            if (typeParameter.isReified) {
                reifiedTypeParameters.addUsedReifiedParameter(typeParameter.name.asString())
            }
        }
        return FunctionCodegen(lambdaInfo.function, codegen.classCodegen).generate(codegen, reifiedTypeParameters)
    }

    override fun compileInlineFunction(jvmSignature: JvmMethodSignature): SMAPAndMethodNode {
        generateInlineIntrinsicForIr(state.languageVersionSettings, callee.toIrBasedDescriptor())?.let {
            return it
        }
        if (jvmSignature.asmMethod.name != callee.name.asString()) {
            val ktFile = codegen.irFunction.fileParent.getKtFile()
            if (ktFile != null && ktFile.doNotAnalyze == null) {
                state.trackLookup(callee.parentAsClass.kotlinFqName, jvmSignature.asmMethod.name, object : LocationInfo {
                    override val filePath = ktFile.virtualFilePath

                    override val position: Position
                        get() = DiagnosticUtils.getLineAndColumnInPsiFile(
                            ktFile,
                            TextRange(callElement.startOffset, callElement.endOffset)
                        ).let { Position(it.line, it.column) }
                })
            }
        }
        callee.parentClassId?.let {
            return loadCompiledInlineFunction(it, jvmSignature.asmMethod, callee.isSuspend, callee.hasMangledReturnType, state)
        }
        return ClassCodegen.getOrCreate(callee.parentAsClass, codegen.context).generateMethodNode(callee)
    }

    override fun hasFinallyBlocks() = data.hasFinallyBlocks()

    override fun generateFinallyBlocks(finallyNode: MethodNode, curFinallyDepth: Int, returnType: Type, afterReturnLabel: Label, target: Label?) {
        ExpressionCodegen(
            codegen.irFunction, codegen.signature, codegen.frameMap, InstructionAdapter(finallyNode), codegen.classCodegen,
            codegen.inlinedInto, codegen.smap, codegen.reifiedTypeParametersUsages
        ).also {
            it.finallyDepth = curFinallyDepth
        }.generateFinallyBlocksIfNeeded(returnType, afterReturnLabel, data, target)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val isCallInsideSameModuleAsCallee: Boolean
        get() = callee.module == codegen.irFunction.module

    override val isFinallyMarkerRequired: Boolean
        get() = codegen.isFinallyMarkerRequired

    override fun isSuspendLambdaCapturedByOuterObjectOrLambda(name: String): Boolean =
        false // IR does not capture variables through outer this

    override fun getContextLabels(): Map<String, Label?> {
        val result = mutableMapOf<String, Label?>(codegen.irFunction.name.asString() to null)
        for (info in data.infos) {
            if (info !is LoopInfo)
                continue
            result[info.loop.nonLocalReturnLabel(false)] = info.continueLabel
            result[info.loop.nonLocalReturnLabel(true)] = info.breakLabel
        }
        return result
    }

    // TODO: Find a way to avoid using PSI here
    override fun reportSuspensionPointInsideMonitor(stackTraceElement: String) {
        codegen.context.psiErrorBuilder
            .at(callElement.symbol.owner as IrDeclaration)
            .report(SUSPENSION_POINT_INSIDE_MONITOR, stackTraceElement)
    }
}

private tailrec fun IrDeclaration.isInlineOrInsideInline(): Boolean {
    val original = (this as? IrAttributeContainer)?.attributeOwnerId as? IrDeclaration ?: this
    if (original is IrSimpleFunction && original.isInline) return true
    val parent = original.parent
    if (parent !is IrDeclaration) return false
    return parent.isInlineOrInsideInline()
}

internal fun IrLoop.nonLocalReturnLabel(forBreak: Boolean): String = "${label!!}\$${if (forBreak) "break" else "continue"}"
