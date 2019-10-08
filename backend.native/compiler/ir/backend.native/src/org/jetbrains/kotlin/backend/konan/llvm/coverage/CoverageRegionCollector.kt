/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.llvm.coverage

import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Collect all regions in the module.
 * @param fileFilter filters files that should be processed.
 */
internal class CoverageRegionCollector(private val fileFilter: (IrFile) -> Boolean) {

    fun collectFunctionRegions(irModuleFragment: IrModuleFragment): List<FileRegionInfo> =
            irModuleFragment.files
                    .filter(fileFilter)
                    .map { file ->
                        val collector = FunctionsCollector(file)
                        collector.visitFile(file)
                        FileRegionInfo(file, collector.functionRegions)
                    }

    private inner class FunctionsCollector(val file: IrFile) : IrElementVisitorVoid {

        val functionRegions = mutableListOf<FunctionRegions>()

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitFunction(declaration: IrFunction) {
            if (!declaration.isInline && !declaration.isExternal && !declaration.isGeneratedByCompiler) {
                declaration.body?.let {
                    val regionsCollector = IrFunctionRegionsCollector(fileFilter, file)
                    regionsCollector.visitBody(it)
                    if (regionsCollector.regions.isNotEmpty()) {
                        functionRegions += FunctionRegions(declaration, regionsCollector.regions)
                    }
                }
            }
            // TODO: Decide how to work with local functions. Should they be process separately?
            declaration.acceptChildrenVoid(this)
        }
    }
}

// User doesn't bother about compiler-generated declarations.
// So lets filter them.
private val IrDeclaration.isGeneratedByCompiler: Boolean
    get() {
        return origin != IrDeclarationOrigin.DEFINED
    }

/**
 * Very similar to [org.jetbrains.kotlin.backend.konan.llvm.CodeGeneratorVisitor] but instead of bitcode generation we collect regions.
 * [fileFilter]: specify which files should be processed by code coverage. Here it is required
 * for checking calls to inline functions from other files.
 * TODO: for now it is very inaccurate.
 */
private class IrFunctionRegionsCollector(
        val fileFilter: (IrFile) -> Boolean,
        val irFile: IrFile
) : IrElementVisitorVoid {

    private val irFileStack = mutableListOf(irFile)

    private val currentFile: IrFile
        get() = irFileStack.last()

    val regions = mutableMapOf<IrElement, Region>()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction) {
    }

    override fun visitExpression(expression: IrExpression) {
        collectRegions(expression)
    }

    override fun visitVariable(declaration: IrVariable) {
        recordRegion(declaration)
        declaration.initializer?.let { collectRegions(it) }
    }

    override fun visitBody(body: IrBody) = when (body) {
        is IrExpressionBody -> body.acceptChildrenVoid(this)
        is IrBlockBody -> body.acceptChildrenVoid(this)
        else -> error("Unexpected function body type: $body")
    }

    fun collectRegions(value: IrExpression): Unit = when (value) {
        is IrTypeOperatorCall -> collectTypeOperator(value)
        is IrCall -> collectCall(value)
        is IrConstructorCall -> collectCall(value)
        is IrDelegatingConstructorCall -> collectCall(value)
        is IrInstanceInitializerCall -> collectInstanceInitializerCall(value)
        is IrGetValue -> collectGetValue(value)
        is IrSetVariable -> collectSetVariable(value)
        is IrGetField -> collectGetField(value)
        is IrSetField -> collectSetField(value)
        is IrConst<*> -> collectConst(value)
        is IrReturn -> collectReturn(value)
        is IrWhen -> collectWhen(value)
        is IrThrow -> collectThrow(value)
        is IrTry -> collectTry(value)
        is IrReturnableBlock -> collectReturnableBlock(value)
        is IrContainerExpression -> collectContainerExpression(value)
        is IrWhileLoop -> collectWhileLoop(value)
        is IrDoWhileLoop -> collectDoWhileLoop(value)
        is IrVararg -> collectVararg(value)
        is IrBreak -> collectBreak(value)
        is IrContinue -> collectContinue(value)
        is IrGetObjectValue -> collectGetObjectValue(value)
        is IrFunctionReference -> collectFunctionReference(value)
        is IrSuspendableExpression -> collectSuspendableExpression(value)
        is IrSuspensionPoint -> collectSuspensionPoint(value)
        else -> {
        }
    }

    private fun collectInstanceInitializerCall(instanceInitializerCall: IrInstanceInitializerCall) {

    }

    private fun collectGetValue(getValue: IrGetValue) {
        recordRegion(getValue)
    }

    private fun collectSetVariable(setVariable: IrSetVariable) {
        recordRegion(setVariable)
        setVariable.value.acceptVoid(this)
    }

    private fun collectGetField(getField: IrGetField) {
        getField.receiver?.let { collectRegions(it) }
    }

    private fun collectSetField(setField: IrSetField) {
        collectRegions(setField.value)
        setField.receiver?.let { collectRegions(it) }
    }

    private fun collectConst(const: IrConst<*>) {
        recordRegion(const)
    }

    private fun collectReturn(irReturn: IrReturn) {
        collectRegions(irReturn.value)
    }

    private fun collectWhen(irWhen: IrWhen) {
        irWhen.branches.forEach { branch ->
            // Do not record location for else branch since it doesn't look correct.
            if (branch.condition !is IrConst<*>) {
                collectRegions(branch.condition)
            }
            collectRegions(branch.result)
        }
    }

    private fun collectThrow(irThrow: IrThrow) {
        collectRegions(irThrow.value)
        recordRegion(irThrow)
    }

    private fun collectTry(irTry: IrTry) {
    }

    private fun collectReturnableBlock(returnableBlock: IrReturnableBlock) {
        val file = (returnableBlock.sourceFileSymbol?.owner)
        if (file != null && file != currentFile && fileFilter(file)) {
            recordRegion(returnableBlock)
            irFileStack.push(file)
            returnableBlock.acceptChildrenVoid(this)
            irFileStack.pop()
        }
    }

    private fun collectContainerExpression(containerExpression: IrContainerExpression) {
        containerExpression.acceptChildrenVoid(this)
    }

    private fun collectWhileLoop(whileLoop: IrWhileLoop) {
        collectRegions(whileLoop.condition)
        whileLoop.body?.let { collectRegions(it) }
    }

    private fun collectDoWhileLoop(doWhileLoop: IrDoWhileLoop) {
        collectRegions(doWhileLoop.condition)
        doWhileLoop.body?.let { collectRegions(it) }
    }

    private fun collectVararg(vararg: IrVararg) {
        vararg.elements.forEach { it.acceptVoid(this) }
    }

    private fun collectBreak(irBreak: IrBreak) {
        recordRegion(irBreak)
    }

    private fun collectContinue(irContinue: IrContinue) {
        recordRegion(irContinue)
    }

    private fun collectGetObjectValue(getObjectValue: IrGetObjectValue) {

    }

    private fun collectFunctionReference(functionReference: IrFunctionReference) {

    }


    private fun collectSuspendableExpression(suspendableExpression: IrSuspendableExpression) {

    }

    private fun collectSuspensionPoint(suspensionPoint: IrSuspensionPoint) {

    }

    private fun collectTypeOperator(typeOperatorCall: IrTypeOperatorCall) {

    }

    private fun collectCall(call: IrFunctionAccessExpression) {
        recordRegion(call, RegionKind.Code)
        call.acceptChildrenVoid(this)
    }

    private fun recordRegion(irElement: IrElement, kind: RegionKind = RegionKind.Code) {
        if (irElement.startOffset == UNDEFINED_OFFSET || irElement.endOffset == UNDEFINED_OFFSET) {
            return
        }
        regions[irElement] = Region.fromIr(irElement, currentFile, kind)
    }
}