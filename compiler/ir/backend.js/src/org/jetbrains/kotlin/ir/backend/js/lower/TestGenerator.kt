/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.name.FqName

fun generateTests(context: JsIrBackendContext, moduleFragment: IrModuleFragment) {
    val generator = TestGenerator(context)

    moduleFragment.files.forEach {
        generator.lower(it)
    }
}

class TestGenerator(val context: JsIrBackendContext) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.declarations.forEach {
            if (it is IrClass) {
                generateTestCalls(it) { suiteForPackage(irFile.fqName).function }
            }

            // TODO top-level functions
        }
    }

    private val packageSuites = mutableMapOf<FqName, FunctionWithBody>()

    private fun suiteForPackage(fqName: FqName) = packageSuites.getOrPut(fqName) {
        context.suiteFun!!.createInvocation(fqName.asString(), context.testContainer)
    }

    private data class FunctionWithBody(val function: IrSimpleFunction, val body: IrBlockBody)

    private fun IrSimpleFunctionSymbol.createInvocation(
        name: String,
        parentFunction: IrSimpleFunction,
        ignored: Boolean = false
    ): FunctionWithBody {
        val body = JsIrBuilder.buildBlockBody(emptyList())

        val function = JsIrBuilder.buildFunction("$name test fun", context.irBuiltIns.anyNType, parentFunction)
        function.body = body

        val parentBody = parentFunction.body as IrBlockBody
        parentBody.statements += JsIrBuilder.buildCall(this).apply {
            putValueArgument(0, JsIrBuilder.buildString(context.irBuiltIns.stringType, name))
            putValueArgument(1, JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, ignored))

            val refType = IrSimpleTypeImpl(context.ir.symbols.functionN(0), false, emptyList(), emptyList())
            putValueArgument(2, JsIrBuilder.buildFunctionExpression(refType, function))
        }

        return FunctionWithBody(function, body)
    }

    private fun generateTestCalls(irClass: IrClass, parentFunction: () -> IrSimpleFunction) {
        if (irClass.modality == Modality.ABSTRACT || irClass.isEffectivelyExternal() || irClass.isExpect) return

        val suiteFunBody by lazy { context.suiteFun!!.createInvocation(irClass.name.asString(), parentFunction(), irClass.isIgnored) }

        val beforeFunctions = irClass.declarations.filterIsInstance<IrSimpleFunction>().filter { it.isBefore }
        val afterFunctions = irClass.declarations.filterIsInstance<IrSimpleFunction>().filter { it.isAfter }

        irClass.declarations.forEach {
            when {
                it is IrClass ->
                    generateTestCalls(it) { suiteFunBody.function }

                it is IrSimpleFunction && it.isTest ->
                    generateCodeForTestMethod(it, beforeFunctions, afterFunctions, irClass, suiteFunBody.function)
            }
        }
    }

    private fun generateCodeForTestMethod(
        testFun: IrSimpleFunction,
        beforeFuns: List<IrSimpleFunction>,
        afterFuns: List<IrSimpleFunction>,
        irClass: IrClass,
        parentFunction: IrSimpleFunction
    ) {
        val (fn, body) = context.testFun!!.createInvocation(testFun.name.asString(), parentFunction, testFun.isIgnored)

        val classVal = JsIrBuilder.buildVar(irClass.defaultType, fn, initializer = irClass.instance())

        body.statements += classVal

        body.statements += beforeFuns.map {
            JsIrBuilder.buildCall(it.symbol).apply {
                dispatchReceiver = JsIrBuilder.buildGetValue(classVal.symbol)
            }
        }

        val returnStatement = JsIrBuilder.buildReturn(
            fn.symbol,
            JsIrBuilder.buildCall(testFun.symbol).apply {
                dispatchReceiver = JsIrBuilder.buildGetValue(classVal.symbol)
            },
            context.irBuiltIns.unitType
        )

        if (afterFuns.isEmpty()) {
            body.statements += returnStatement
        } else {
            body.statements += JsIrBuilder.buildTry(context.irBuiltIns.unitType).apply {
                tryResult = returnStatement
                finallyExpression = JsIrBuilder.buildComposite(context.irBuiltIns.unitType).apply {
                    statements += afterFuns.map {
                        JsIrBuilder.buildCall(it.symbol).apply {
                            dispatchReceiver = JsIrBuilder.buildGetValue(classVal.symbol)
                        }
                    }
                }
            }
        }
    }

    private fun IrClass.instance(): IrExpression {
        return if (kind == ClassKind.OBJECT) {
            JsIrBuilder.buildGetObjectValue(defaultType, symbol)
        } else {
            declarations.asSequence().filterIsInstance<IrConstructor>().single { it.isPrimary }.let { constructor ->
                IrConstructorCallImpl.fromSymbolOwner(defaultType, constructor.symbol).also {
                    if (isInner) {
                        it.dispatchReceiver = (parent as IrClass).instance()
                    }
                }
            }
        }
    }

    private val IrAnnotationContainer.isTest
        get() = hasAnnotation("kotlin.test.Test")

    private val IrAnnotationContainer.isIgnored
        get() = hasAnnotation("kotlin.test.Ignore")

    private val IrAnnotationContainer.isBefore
        get() = hasAnnotation("kotlin.test.BeforeTest")

    private val IrAnnotationContainer.isAfter
        get() = hasAnnotation("kotlin.test.AfterTest")

    private fun IrAnnotationContainer.hasAnnotation(fqName: String) =
        annotations.any { it.symbol.owner.fqNameWhenAvailable?.parent()?.asString() == fqName }
}
