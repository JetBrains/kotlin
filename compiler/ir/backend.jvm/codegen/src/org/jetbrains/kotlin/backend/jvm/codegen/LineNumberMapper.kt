/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.ir.extractDeclarationWhereGivenElementWasInlined
import org.jetbrains.kotlin.ir.util.inlineDeclaration
import org.jetbrains.kotlin.ir.util.isFunctionInlining
import org.jetbrains.kotlin.ir.util.isLambdaInlining
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.fileParentBeforeInline
import org.jetbrains.kotlin.backend.jvm.ir.isInlineOnly
import org.jetbrains.kotlin.codegen.inline.SMAP
import org.jetbrains.kotlin.codegen.inline.SourceMapCopier
import org.jetbrains.kotlin.codegen.inline.SourceMapper
import org.jetbrains.kotlin.codegen.inline.SourcePosition
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.util.fileEntry
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.org.objectweb.asm.Label
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * This class serves two purposes:
 * 1. Generate line numbers for given `IrElement`
 * 2. Keep track of `smap` for functions inlined by IR inliner. We can consider this class as basically just another wrapper around SMAP.
 * It is used to unify smap creation for functions inlined from IR and from bytecode.
 */
class LineNumberMapper(private val expressionCodegen: ExpressionCodegen) {
    private val context: JvmBackendContext
        get() = expressionCodegen.context
    private val smap = expressionCodegen.smap
    private val irFunction = expressionCodegen.irFunction
    private val fileEntry = irFunction.fileParentBeforeInline.fileEntry

    private var lastLineNumber: Int = -1
    private var noLineNumberScope: Boolean = false

    private val smapStack = mutableListOf<DataForIrInlinedFunction>()

    private data class DataForIrInlinedFunction(
        val smap: SourceMapCopier,
        val inlinedBlock: IrInlinedFunctionBlock,
        val parentSmap: SourceMapper, // this property is used to simulate change in smap but without ruining previous one
        val tryInfo: TryWithFinallyInfo?
    )

    private fun markNewLabel() = Label().apply { expressionCodegen.mv.visitLabel(this) }

    fun markLineNumber(element: IrElement, startOffset: Boolean) {
        if (noLineNumberScope) return
        val offset = if (startOffset) element.startOffset else element.endOffset
        if (offset < 0) return

        val lineNumber = getLineNumberForOffset(offset)

        assert(lineNumber > 0)
        if (lastLineNumber != lineNumber) {
            lastLineNumber = lineNumber
            expressionCodegen.mv.visitLineNumber(lineNumber, markNewLabel())
        }
    }

    @OptIn(ExperimentalContracts::class)
    internal inline fun noLineNumberScopeWithCondition(flag: Boolean, block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        val previousState = noLineNumberScope
        noLineNumberScope = noLineNumberScope || flag
        block()
        noLineNumberScope = previousState
    }

    fun noLineNumberScope(block: () -> Unit) {
        val previousState = noLineNumberScope
        noLineNumberScope = true
        block()
        noLineNumberScope = previousState
    }

    fun markLineNumberAfterInlineIfNeeded(registerLineNumberAfterwards: Boolean) {
        if (noLineNumberScope || registerLineNumberAfterwards) {
            if (lastLineNumber > -1) {
                val label = Label()
                expressionCodegen.mv.visitLabel(label)
                expressionCodegen.mv.visitLineNumber(lastLineNumber, label)
            }
        } else {
            // Inline function has its own line number which is in a separate instance of codegen,
            // therefore we need to reset lastLineNumber to force a line number generation after visiting inline function.
            lastLineNumber = -1
        }
    }

    fun getLineNumber(): Int {
        return lastLineNumber
    }

    fun resetLineNumber() {
        lastLineNumber = -1
    }

    fun beforeIrInline(inlinedBlock: IrInlinedFunctionBlock) {
        if (inlinedBlock.isLambdaInlining()) {
            setUpAdditionalLineNumbersBeforeLambdaInlining(inlinedBlock)
        }
    }

    fun afterIrInline(inlinedBlock: IrInlinedFunctionBlock) {
        if (inlinedBlock.isFunctionInlining()) {
            val callLineNumber = getLineNumberForOffset(inlinedBlock.inlineCall.startOffset)
            // `takeUnless` is required to avoid `markLineNumberAfterInlineIfNeeded` for inline only
            lastLineNumber = callLineNumber.takeUnless { noLineNumberScope } ?: -1
            markLineNumberAfterInlineIfNeeded(expressionCodegen.isInsideCondition)
        } else {
            setUpAdditionalLineNumbersAfterLambdaInlining(inlinedBlock)
        }
    }

    fun dropCurrentSmap() {
        smapStack.removeFirst()
    }

    private fun getLineNumberForOffset(offset: Int): Int {
        if (smapStack.isEmpty()) {
            return fileEntry.getLineNumber(offset) + 1
        }

        var previousData: DataForIrInlinedFunction? = null
        var result = -1
        val iterator = smapStack.iterator()
        while (iterator.hasNext()) {
            if (previousData != null && previousData.inlinedBlock.isLambdaInlining()) {
                val inlinedAt = getDeclarationWhereGivenElementWasInlined(previousData.inlinedBlock.inlinedElement)
                while (iterator.hasNext() && iterator.next().inlinedBlock.inlineDeclaration != inlinedAt) {
                    // after lambda's smap we should skip "frames" that were inlined inside body of inline function that accept given lambda
                    continue
                }
                if (!iterator.hasNext()) break
            }
            val inlineData = iterator.next()

            previousData = inlineData
            val localFileEntry = inlineData.inlinedBlock.getClassThatContainsDeclaration().fileParentBeforeInline.fileEntry
            val lineNumber = if (result == -1) localFileEntry.getLineNumber(offset) + 1 else result
            val mappedLineNumber = inlineData.smap.mapLineNumber(lineNumber)
            result = mappedLineNumber
        }

        return result
    }

    fun buildSmapFor(inlinedBlock: IrInlinedFunctionBlock, classSMAP: SMAP, data: BlockInfo) {
        val inlineCall = inlinedBlock.inlineCall

        val newData = if (inlinedBlock.isLambdaInlining()) {
            val callSite = smapStack.firstOrNull()?.smap?.callSite?.takeIf { inlinedBlock.isInvokeOnDefaultArg() }

            val sourceMapper = if (smapStack.isEmpty()) {
                smap
            } else {
                val inlinedAt = getDeclarationWhereGivenElementWasInlined(inlinedBlock.inlinedElement)
                val inlineData = smapStack.firstOrNull { it.inlinedBlock.inlineDeclaration == inlinedAt }
                    ?: smapStack.first() // if we are in anonymous inlined class and lambda was declared outside
                inlineData.smap.parent
            }
            DataForIrInlinedFunction(
                SourceMapCopier(sourceMapper, classSMAP, callSite),
                inlinedBlock,
                context.getSourceMapper(inlinedBlock.getClassThatContainsDeclaration()),
                data.infos.filterIsInstance<TryWithFinallyInfo>().lastOrNull(),
            )
        } else {
            val sourceMapper = if (smapStack.isEmpty()) smap else smapStack.first().parentSmap
            val sourcePosition = let {
                val sourceInfo = sourceMapper.sourceInfo!!
                val localFileEntry = smapStack.firstOrNull()?.inlinedBlock?.inlineDeclaration?.fileParentBeforeInline?.fileEntry ?: fileEntry
                val line = if (inlineCall.startOffset < 0) lastLineNumber else localFileEntry.getLineNumber(inlineCall.startOffset) + 1
                SourcePosition(line, sourceInfo.sourceFileName!!, sourceInfo.pathOrCleanFQN)
            }

            DataForIrInlinedFunction(
                SourceMapCopier(sourceMapper, classSMAP, sourcePosition),
                inlinedBlock,
                context.getSourceMapper(inlinedBlock.getClassThatContainsDeclaration()),
                data.infos.filterIsInstance<TryWithFinallyInfo>().lastOrNull(),
            )
        }

        smapStack.add(0, newData)
    }

    fun stashSmapForGivenTry(tryInfo: TryWithFinallyInfo, block: () -> Unit) {
        val lastLineNumberBeforeFinally = lastLineNumber
        val smapCountToDrop = smapStack.indexOfLast { it.tryInfo == tryInfo } + 1
        if (smapCountToDrop == 0) {
            return block()
        }

        val smapInTryBlock = smapStack.take(smapCountToDrop)
        smapInTryBlock.forEach { _ -> dropCurrentSmap() }
        block()
        smapInTryBlock.reversed().forEach { smapStack.add(0, it) }
        if (smapInTryBlock.isNotEmpty()) {
            lastLineNumber = lastLineNumberBeforeFinally
        }
    }

    private fun setUpAdditionalLineNumbersBeforeLambdaInlining(inlinedBlock: IrInlinedFunctionBlock) {
        val lineNumberForOffset = getLineNumberForOffset(inlinedBlock.inlineCall.startOffset)
        val callee = inlinedBlock.inlineDeclaration as? IrFunction

        // TODO: reuse code from org/jetbrains/kotlin/codegen/inline/MethodInliner.kt:267
        val overrideLineNumber = smapStack
            .map { it.inlinedBlock }
            .firstOrNull { !it.isLambdaInlining() }?.inlineDeclaration?.isInlineOnly() == true
        val currentLineNumber = if (overrideLineNumber) smapStack.first().smap.callSite!!.line else lineNumberForOffset

        val firstLine = callee?.body?.statements?.firstOrNull()?.let {
            inlinedBlock.inlineDeclaration.fileEntry.getLineNumber(it.startOffset) + 1
        } ?: -1
        if ((inlinedBlock.isInvokeOnDefaultArg() != overrideLineNumber) && currentLineNumber >= 0 && firstLine == currentLineNumber) {
            val label = Label()
            val fakeLineNumber = (smapStack.firstOrNull()?.smap?.parent ?: smap)
                .mapSyntheticLineNumber(SourceMapper.LOCAL_VARIABLE_INLINE_ARGUMENT_SYNTHETIC_LINE_NUMBER)
            expressionCodegen.mv.visitLabel(label)
            expressionCodegen.mv.visitLineNumber(fakeLineNumber, label)
        }
    }

    private fun setUpAdditionalLineNumbersAfterLambdaInlining(inlinedBlock: IrInlinedFunctionBlock) {
        val lineNumberForOffset = getLineNumberForOffset(inlinedBlock.inlineCall.startOffset)

        // TODO: reuse code from org/jetbrains/kotlin/codegen/inline/MethodInliner.kt:316
        val overrideLineNumber = smapStack
            .map { it.inlinedBlock }
            .firstOrNull { !it.isLambdaInlining() }?.inlineDeclaration?.isInlineOnly() == true
        val currentLineNumber = if (overrideLineNumber) smapStack.first().smap.callSite!!.line else lineNumberForOffset
        if (currentLineNumber != -1) {
            if (overrideLineNumber) {
                // This is from the function we're inlining into, so no need to remap.
                expressionCodegen.mv.visitLineNumber(currentLineNumber, markNewLabel())
            } else {
                // Need to go through the superclass here to properly remap the line number via `sourceMapper`.
                markLineNumber(inlinedBlock.inlineCall, startOffset = true)
            }
            expressionCodegen.mv.nop()
        }
    }

    private fun IrInlinedFunctionBlock.getClassThatContainsDeclaration(): IrClass {
        val firstFunctionInlineBlock = if (this.inlinedElement is IrCallableReference<*>)
            smapStack.map { it.inlinedBlock }.firstOrNull { it.isFunctionInlining() }
        else
            this

        return firstFunctionInlineBlock?.inlineDeclaration?.parentClassOrNull ?: irFunction.parentAsClass
    }

    private fun getDeclarationWhereGivenElementWasInlined(inlinedElement: IrElement): IrDeclaration? {
        return smapStack.map { it.inlinedBlock }.extractDeclarationWhereGivenElementWasInlined(inlinedElement)
    }

    private fun IrInlinedFunctionBlock.isInvokeOnDefaultArg(): Boolean {
        val call = this.inlineCall
        val expected = this.inlineDeclaration
        if (call.symbol.owner.name != OperatorNameConventions.INVOKE) return false

        val dispatch = call.dispatchReceiver as? IrGetValue
        val parameter = dispatch?.symbol?.owner as? IrValueParameter
        val default = parameter?.defaultValue?.expression as? IrFunctionExpression

        return default?.function == expected
    }
}