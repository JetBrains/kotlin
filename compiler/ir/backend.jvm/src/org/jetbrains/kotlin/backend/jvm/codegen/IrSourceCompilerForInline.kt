/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.codegen.BaseExpressionCodegen
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.incremental.components.LocationInfo
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBasedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.module
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.doNotAnalyze
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.SUSPENSION_POINT_INSIDE_MONITOR
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class IrSourceCompilerForInline(
    override val state: GenerationState,
    override val callElement: IrFunctionAccessExpression,
    private val callee: IrFunction,
    internal val codegen: ExpressionCodegen,
    private val data: BlockInfo
) : SourceCompilerForInline {

    override val lookupLocation: LookupLocation
        get() = object : LookupLocation {
            override val location: LocationInfo?
                get() {
                    val ktFile = codegen.classCodegen.context.psiSourceManager.getKtFile(codegen.irFunction.fileParent)
                        ?.takeUnless { it.doNotAnalyze != null } ?: return null

                    return object : LocationInfo {
                        override val filePath = ktFile.virtualFilePath

                        override val position: Position
                            get() = DiagnosticUtils.getLineAndColumnInPsiFile(
                                ktFile,
                                TextRange(callElement.startOffset, callElement.endOffset)
                            ).let { Position(it.line, it.column) }
                    }
                }
        }

    override val callElementText: String
        get() = ir2string(callElement)

    override val callsiteFile: PsiFile?
        get() = codegen.context.psiSourceManager.getKtFile(codegen.irFunction.fileParent)

    override val contextKind: OwnerKind
        get() = when (val parent = callElement.symbol.owner.parent) {
            is IrPackageFragment -> OwnerKind.PACKAGE
            is IrClass -> OwnerKind.IMPLEMENTATION
            else -> throw AssertionError("Unexpected declaration container: $parent")
        }

    override val inlineCallSiteInfo: InlineCallSiteInfo
        get() {
            val root = generateSequence(codegen) { it.inlinedInto }.last()
            return InlineCallSiteInfo(
                root.classCodegen.type.internalName,
                root.signature.asmMethod.name,
                root.signature.asmMethod.descriptor,
                root.irFunction.isInlineOrInsideInline(),
                root.irFunction.isSuspend,
                findElement()?.let { CodegenUtil.getLineNumberForElement(it, false) } ?: 0
            )
        }

    override val lazySourceMapper: SourceMapper
        get() = codegen.smap

    override fun generateLambdaBody(lambdaInfo: ExpressionLambda): SMAPAndMethodNode =
        FunctionCodegen((lambdaInfo as IrExpressionLambdaImpl).function, codegen.classCodegen, codegen).generate()

    override fun doCreateMethodNodeFromSource(
        callableDescriptor: FunctionDescriptor,
        jvmSignature: JvmMethodSignature,
        callDefault: Boolean,
        asmMethod: Method
    ): SMAPAndMethodNode =
        ClassCodegen.getOrCreate(callee.parentAsClass, codegen.context).generateMethodNode(callee)

    override fun hasFinallyBlocks() = data.hasFinallyBlocks()

    override fun generateFinallyBlocksIfNeeded(codegen: BaseExpressionCodegen, returnType: Type, afterReturnLabel: Label, target: Label?) {
        require(codegen is ExpressionCodegen)
        codegen.generateFinallyBlocksIfNeeded(returnType, afterReturnLabel, data, target)
    }

    override fun createCodegenForExternalFinallyBlockGenerationOnNonLocalReturn(finallyNode: MethodNode, curFinallyDepth: Int) =
        ExpressionCodegen(
            codegen.irFunction, codegen.signature, codegen.frameMap, InstructionAdapter(finallyNode), codegen.classCodegen,
            codegen.inlinedInto, codegen.smap
        ).also {
            it.finallyDepth = curFinallyDepth
        }

    @ObsoleteDescriptorBasedAPI
    override fun isCallInsideSameModuleAsDeclared(functionDescriptor: FunctionDescriptor): Boolean {
        require(functionDescriptor is IrBasedSimpleFunctionDescriptor) {
            "expected an IrBasedSimpleFunctionDescriptor, got $functionDescriptor"
        }
        val function = functionDescriptor.owner
        return function.module == codegen.irFunction.module
    }

    override fun isFinallyMarkerRequired(): Boolean {
        return codegen.isFinallyMarkerRequired()
    }

    override val compilationContextDescriptor: DeclarationDescriptor
        get() = compilationContextFunctionDescriptor

    override val compilationContextFunctionDescriptor: FunctionDescriptor
        get() = generateSequence(codegen) { it.inlinedInto }.last().irFunction.toIrBasedDescriptor()

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

    private fun findElement() = callElement.psiElement as? KtElement
}

private tailrec fun IrDeclaration.isInlineOrInsideInline(): Boolean {
    val original = (this as? IrAttributeContainer)?.attributeOwnerId as? IrDeclaration ?: this
    if (original is IrSimpleFunction && original.isInline) return true
    val parent = original.parent
    if (parent !is IrDeclaration) return false
    return parent.isInlineOrInsideInline()
}

// TODO generate better labels; this is unique (includes the object's address), but not very descriptive
internal fun IrLoop.nonLocalReturnLabel(forBreak: Boolean): String = "$this\$${if (forBreak) "break" else "continue"}"
