/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.webWorkers

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.isTopLevelInPackage
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.sure

private data class WorkerInfo(val call: IrCall, val lambda: IrFunction, val index: Int)

private const val WORKER_FUNCTION_NAME_PREFIX = "__Worker_"

class WorkerFileWithIndex(val irFile: IrFile, val index: Int) {
    val functionName = WORKER_FUNCTION_NAME_PREFIX + index
}

fun prepareFilePrefixForWorkers(): String = "var wt = require('worker_threads');\n"

fun prepareFileSuffixForWorkers(moduleName: String, workerFileWithIndices: List<WorkerFileWithIndex>): String {
    val res = StringBuffer(
        """
if (!wt.isMainThread) {
    switch (wt.workerData.id) {"""
    )
    for (workerFileWithIndex in workerFileWithIndices) {
        res.append(
            """
        case ${workerFileWithIndex.index}:
            wt.parentPort.on('message', $moduleName.${workerFileWithIndex.functionName});
            break;
"""
        )
    }
    res.append(
        """
        default:
            throw Error("Worker with id: " + id + " not found")
    }
}
"""
    )
    return res.toString()
}

fun moveWorkersToSeparateFiles(irFile: IrFile, context: JsIrBackendContext): List<WorkerFileWithIndex> {
    val workerCalls = irFile.filterCalls { it.descriptor.isWorker() }

    if (workerCalls.isEmpty()) return emptyList()

    val infos = workerCalls.withIndex().map { (index, call) ->
        WorkerInfo(
            call,
            (call.getValueArgument(0) as? IrFunctionExpression ?: error("worker intrinsic accepts only block, but got $call")).function,
            index
        )
    }

    val result = infos.map { (_, workerLambda, index) ->
        replaceReturnsWithPostMessage(workerLambda, context)
        WorkerFileWithIndex(moveToSeparateFile(workerLambda, context, "$WORKER_FUNCTION_NAME_PREFIX$index"), index)
    }

    replaceWorkerIntrinsicCalls(infos, irFile, context)
    return result
}

private fun moveToSeparateFile(
    workerLambda: IrFunction,
    context: JsIrBackendContext,
    fileName: String
): IrFile {
    val newFile = IrFileImpl(
        object : SourceManager.FileEntry {
            override val name = fileName
            override val maxOffset = UNDEFINED_OFFSET

            override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int) =
                SourceRangeInfo(
                    "",
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET
                )

            override fun getLineNumber(offset: Int) = UNDEFINED_OFFSET
            override fun getColumnNumber(offset: Int) = UNDEFINED_OFFSET
        }, context.workersPackageFragmentDescriptor
    )
    val function = JsIrBuilder.buildFunction(fileName, context.irBuiltIns.unitType, newFile).also {
        it.body = workerLambda.body
        it.copyValueParametersAndUpdateGetValues(workerLambda)
    }
    newFile.declarations.add(function)
    return newFile
}

fun IrFunction.copyValueParametersAndUpdateGetValues(original: IrFunction) {
    for (param in original.valueParameters) {
        addValueParameter(param.name.asString(), param.type)
    }
    body?.transformChildrenVoid(object : IrElementTransformerVoid() {
        override fun visitGetValue(expression: IrGetValue): IrExpression {
            val index = original.valueParameters.single { it.symbol == expression.symbol.owner }.index
            return IrGetValueImpl(expression.startOffset, expression.endOffset, valueParameters[index].symbol)
        }
    })
}

private fun replaceReturnsWithPostMessage(workerLambda: IrFunction, context: JsIrBackendContext) {
    workerLambda.body.sure { "worker lambda $workerLambda shall have body" }.transformReturns {
        IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.unitType, context.postMessage).apply {
            putValueArgument(0, it.value)
        }
    }
}

private fun replaceWorkerIntrinsicCalls(
    infos: List<WorkerInfo>,
    irFile: IrFile,
    context: JsIrBackendContext
) {
    irFile.transformCalls { call ->
        val info = infos.find { it.call == call }
        if (info != null) {
            val constructor = context.workerClass.constructors.first()
            IrConstructorCallImpl.fromSymbolDescriptor(
                call.startOffset, call.endOffset, context.workerClass.createType(false, emptyList()),
                constructor
            ).apply {
                putValueArgument(
                    0, newCallWithUndefinedOffsets(
                        symbol = context.jsCodeSymbol,
                        arguments = listOf(context.string("new wt.Worker(__filename, { workerData: { id: ${info.index} } })"))
                    )
                )
                putValueArgument(
                    1, newCallWithUndefinedOffsets(
                        symbol = context.jsCodeSymbol,
                        arguments = listOf(context.string("{}"))
                    )
                )
            }
        } else call
    }
}

fun IrElement.filterCalls(predicate: (IrCall) -> Boolean): List<IrCall> {
    val res = arrayListOf<IrCall>()
    acceptVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitCall(expression: IrCall) {
            if (predicate(expression)) {
                res += expression
            }
            super.visitCall(expression)
        }
    })
    return res
}

fun IrElement.transformCalls(transformer: (IrCall) -> IrExpression) {
    transformChildrenVoid(object : IrElementTransformerVoid() {
        override fun visitCall(expression: IrCall): IrExpression {
            val visited = super.visitCall(expression) as IrCall
            return transformer(visited)
        }
    })
}

fun IrElement.transformReturns(transformer: (IrReturn) -> IrExpression) {
    transformChildrenVoid(object : IrElementTransformerVoid() {
        override fun visitReturn(expression: IrReturn): IrExpression {
            val visited = super.visitReturn(expression) as IrReturn
            return transformer(visited)
        }
    })
}

fun newCallWithUndefinedOffsets(
    symbol: IrFunctionSymbol,
    arguments: List<IrExpression?>
) = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol.owner.returnType, symbol).apply {
    for ((index, expression) in arguments.withIndex()) {
        putValueArgument(index, expression)
    }
}

private val JsIrBackendContext.workersPackageFragmentDescriptor: PackageFragmentDescriptor
    get() = EmptyPackageFragmentDescriptor(builtIns.builtInsModule, FqName(""))

private fun FunctionDescriptor.isWorker(): Boolean = isTopLevelInPackage("worker", "kotlin.js.worker")

private fun JsIrBackendContext.string(s: String) = JsIrBuilder.buildString(irBuiltIns.stringType, s)
private val JsIrBackendContext.jsCodeSymbol
    get() = symbolTable.referenceSimpleFunction(getJsInternalFunction("js"))

