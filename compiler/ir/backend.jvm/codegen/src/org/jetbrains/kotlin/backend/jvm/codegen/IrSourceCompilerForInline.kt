/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.jvm.hasMangledReturnType
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.incremental.components.LocationInfo
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.psi.doNotAnalyze
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmBackendErrors
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
            val rootFunction = codegen.enclosingFunctionForLocalObjects
            return InlineCallSiteInfo(
                codegen.classCodegen.type.internalName,
                if (rootFunction === codegen.irFunction)
                    codegen.signature.asmMethod
                else
                    codegen.methodSignatureMapper.mapAsmMethod(rootFunction),
                rootFunction.inlineScopeVisibility,
                rootFunction.fileParent.getIoFile(),
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
        return FunctionCodegen(lambdaInfo.function, codegen.classCodegen).generate(reifiedTypeParameters)
    }

    override fun compileInlineFunction(jvmSignature: JvmMethodSignature): SMAPAndMethodNode {
        generateInlineIntrinsicForIr(callee.toIrBasedDescriptor())?.let {
            return it
        }
        if (jvmSignature.asmMethod.name != callee.name.asString()) {
            val ktFile = codegen.irFunction.fileParent.getKtFile()
            if ((ktFile != null && ktFile.doNotAnalyze == null) || ktFile == null) {
                state.trackLookup(callee.parentAsClass.kotlinFqName, jvmSignature.asmMethod.name, object : LocationInfo {
                    override val filePath = ktFile?.virtualFile?.path ?: codegen.irFunction.fileParent.fileEntry.name

                    override val position: Position
                        get() =
                            if (ktFile == null) Position.NO_POSITION
                            else DiagnosticUtils.getLineAndColumnInPsiFile(
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
            sourceMapper, codegen.reifiedTypeParametersUsages
        ).also {
            it.finallyDepth = curFinallyDepth
        }.generateFinallyBlocksIfNeeded(returnType, afterReturnLabel, data, target)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val isCallInsideSameModuleAsCallee: Boolean
        get() {
            val inlineFunModule = callee.fileOrNull?.module
            val currentlyGeneratedFunModule = codegen.irFunction.fileOrNull?.module
            check(currentlyGeneratedFunModule != null) {
                "There is no module for function ${codegen.irFunction.name}:\n${codegen.irFunction.render()}"
            }

            return if (inlineFunModule == null) {
                callee.module == codegen.irFunction.module
            } else {
                // Check by IR is needed for the evaluate expression in IDE.
                // When we compile some code fragment with inline function call
                // that has an anonymous object in callee, we will get incorrect behavior.
                // Code fragment is wrapped in `EvaluatorModuleDescriptor` and we accidentally
                // think that inline call and callee are in different modules that leads to an error in
                // `AnonymousObjectTransformer.doTransform`.
                inlineFunModule == currentlyGeneratedFunModule
            }
        }

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
        codegen.context.ktDiagnosticReporter
            .at(callElement.symbol.owner as IrDeclaration)
            .report(JvmBackendErrors.SUSPENSION_POINT_INSIDE_MONITOR, stackTraceElement)
    }
}

internal fun IrLoop.nonLocalReturnLabel(forBreak: Boolean): String = "${label!!}\$${if (forBreak) "break" else "continue"}"
