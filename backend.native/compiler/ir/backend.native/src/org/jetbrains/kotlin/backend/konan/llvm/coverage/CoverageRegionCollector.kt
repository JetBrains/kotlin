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
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.ir.util.statements
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
                val regionsCollector = IrFunctionRegionsCollector(fileFilter, file)
                declaration.acceptVoid(regionsCollector)
                if (regionsCollector.regions.isNotEmpty()) {
                    functionRegions += FunctionRegions(declaration, regionsCollector.regions)
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
        return origin != IrDeclarationOrigin.DEFINED || nameForIrSerialization.asString() == "Konan_start"
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

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        declaration.body?.let {
            recordRegion(it)
            it.acceptChildrenVoid(this)
        }
    }

    override fun visitConstructor(declaration: IrConstructor) {
        val statements = declaration.body?.statements ?: return
        statements.forEach {
            if (it is IrDelegatingConstructorCall && !declaration.isPrimary
                    || it !is IrDelegatingConstructorCall && it !is IrReturn) {
                recordRegion(it)
                it.acceptVoid(this)
            }
        }
    }

    // TODO: The following implementation produces correct region mapping, but something goes wrong later
    // override fun visitFieldAccess(expression: IrFieldAccessExpression) {
    //     expression.receiver?.let { recordRegion(it) }
    //     expression.acceptChildrenVoid(this)
    // }

    override fun visitWhen(expression: IrWhen) {
        val branches = expression.branches
        branches.forEach {
            val condition = it.condition
            val result = it.result

            if (it is IrElseBranch) {
                recordRegion(result)
            } else {
                recordRegion(condition)
                recordRegion(result, condition.endOffset, result.endOffset)
                condition.acceptChildrenVoid(this)
            }
            result.acceptChildrenVoid(this)
        }
    }

    override fun visitLoop(loop: IrLoop) {
        val condition = loop.condition
        recordRegion(condition)
        condition.acceptChildrenVoid(this)

        val body = loop.body ?: return
        when (loop) {
            is IrWhileLoop -> recordRegion(body, condition.endOffset, body.endOffset)
            is IrDoWhileLoop -> recordRegion(body, body.startOffset, condition.startOffset)
        }
        body.acceptChildrenVoid(this)
    }

    override fun visitBlock(expression: IrBlock) {
        when (expression) {
            is IrReturnableBlock -> {
                val file = (expression.sourceFileSymbol?.owner)
                if (file != null && file != currentFile && fileFilter(file)) {
                    recordRegion(expression)
                    irFileStack.push(file)
                    expression.acceptChildrenVoid(this)
                    irFileStack.pop()
                }
            }
            else -> expression.acceptChildrenVoid(this)
        }
    }

    private fun recordRegion(irElement: IrElement, kind: RegionKind = RegionKind.Code) {
        recordRegion(irElement, irElement.startOffset, irElement.endOffset, kind)
    }

    private fun recordRegion(irElement: IrElement, startOffset: Int, endOffset: Int, kind: RegionKind = RegionKind.Code) {
        if (startOffset == UNDEFINED_OFFSET || endOffset == UNDEFINED_OFFSET) {
            return
        }
        regions[irElement] = Region.fromOffset(startOffset, endOffset, currentFile, kind)
    }
}