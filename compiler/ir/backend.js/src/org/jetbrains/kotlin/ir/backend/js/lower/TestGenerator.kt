/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.filterIsInstanceAnd
import org.jetbrains.kotlin.utils.findIsInstanceAnd
import org.jetbrains.kotlin.utils.memoryOptimizedMap

fun generateJsTests(context: JsIrBackendContext, moduleFragment: IrModuleFragment) {
    val generator = TestGenerator(context, false)

    moduleFragment.files.toList().forEach {
        generator.lower(it)
    }
}

class TestGenerator(val context: JsCommonBackendContext, val groupByPackage: Boolean) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        // Additional copy to prevent ConcurrentModificationException
        if (irFile.declarations.isEmpty()) return
        ArrayList(irFile.declarations).forEach {
            if (it is IrClass) {
                generateTestCalls(it) { if (groupByPackage) suiteForPackage(irFile) else context.createTestContainerFun(irFile) }
            }

            // TODO top-level functions
        }
    }

    private val packageSuites = hashMapOf<FqName, IrSimpleFunction>()

    private fun suiteForPackage(irFile: IrFile) = packageSuites.getOrPut(irFile.packageFqName) {
        context.suiteFun!!.createInvocation(irFile.packageFqName.asString(), context.createTestContainerFun(irFile))
    }

    private fun IrSimpleFunctionSymbol.createInvocation(
        name: String,
        parentFunction: IrSimpleFunction,
        ignored: Boolean = false
    ): IrSimpleFunction {
        val body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, emptyList())

        val function = context.irFactory.buildFun {
            this.name = Name.identifier("$name test fun")
            this.returnType = if (this@createInvocation == context.suiteFun!!) context.irBuiltIns.unitType else context.irBuiltIns.anyNType
            this.origin = JsIrBuilder.SYNTHESIZED_DECLARATION
        }
        function.parent = parentFunction
        function.body = body

        (parentFunction.body as IrBlockBody).statements += JsIrBuilder.buildCall(this).apply {
            putValueArgument(0, JsIrBuilder.buildString(context.irBuiltIns.stringType, name))
            putValueArgument(1, JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, ignored))

            val refType = IrSimpleTypeImpl(context.ir.symbols.functionN(0), false, emptyList(), emptyList())
            putValueArgument(2, JsIrBuilder.buildFunctionExpression(refType, function))
        }

        return function
    }

    private fun generateTestCalls(irClass: IrClass, parentFunction: () -> IrSimpleFunction) {
        if (irClass.modality == Modality.ABSTRACT || irClass.isEffectivelyExternal() || irClass.isExpect) return

        val suiteFunBody by lazy(LazyThreadSafetyMode.NONE) {
            context.suiteFun!!.createInvocation(irClass.name.asString(), parentFunction(), irClass.isIgnored)
        }

        val beforeFunctions = irClass.declarations.filterIsInstanceAnd<IrSimpleFunction> { it.isBefore }
        val afterFunctions = irClass.declarations.filterIsInstanceAnd<IrSimpleFunction> { it.isAfter }

        irClass.declarations.forEach {
            when {
                it is IrClass ->
                    generateTestCalls(it) { suiteFunBody }

                it is IrSimpleFunction && it.isTest ->
                    generateCodeForTestMethod(it, beforeFunctions, afterFunctions, irClass, suiteFunBody)
            }
        }
    }

    private fun IrDeclarationWithVisibility.isVisibleFromTests() =
        (visibility == DescriptorVisibilities.PUBLIC) || (visibility == DescriptorVisibilities.INTERNAL)

    private fun IrDeclarationWithVisibility.isEffectivelyVisibleFromTests(): Boolean {
        return generateSequence(this) { it.parent as? IrDeclarationWithVisibility }.all {
            it.isVisibleFromTests()
        }
    }

    private fun IrClass.canBeInstantiated(): Boolean {
        val isClassReachable = isEffectivelyVisibleFromTests()
        return if (isObject) {
            isClassReachable
        } else {
            isClassReachable && constructors.any {
                it.isVisibleFromTests() && it.explicitParametersCount == if (isInner) 1 else 0
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
        val fn = context.testFun!!.createInvocation(testFun.name.asString(), parentFunction, testFun.isIgnored)
        val body = fn.body as IrBlockBody

        val exceptionMessage = when {
            testFun.valueParameters.isNotEmpty() || !testFun.isEffectivelyVisibleFromTests() ->
                "Test method ${irClass.fqNameWhenAvailable ?: irClass.name}::${testFun.name} should have public or internal visibility, can not have parameters"
            !irClass.canBeInstantiated() ->
                "Test class ${irClass.fqNameWhenAvailable ?: irClass.name} must declare a public or internal constructor with no explicit parameters"
            else -> null
        }

        if (exceptionMessage != null) {
            val irBuilder = context.createIrBuilder(fn.symbol)
            body.statements += irBuilder.irCall(context.irBuiltIns.illegalArgumentExceptionSymbol).apply {
                putValueArgument(0, irBuilder.irString(exceptionMessage))
            }

            return
        }

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
            return
        }

        if (context is JsIrBackendContext && (testFun.returnType as? IrSimpleType)?.classifier == context.intrinsics.promiseClassSymbol) {
            val finally = context.intrinsics.promiseClassSymbol.owner.declarations
                .findIsInstanceAnd<IrSimpleFunction> { it.name.asString() == "finally" }!!

            val refType = IrSimpleTypeImpl(context.ir.symbols.functionN(0), false, emptyList(), emptyList())

            val afterFunction = context.irFactory.buildFun {
                this.name = Name.identifier("${irClass.name.asString()} after test fun")
                this.returnType = context.irBuiltIns.unitType
                this.origin = JsIrBuilder.SYNTHESIZED_DECLARATION
            }.apply {
                parent = fn
                this.body = context.irFactory.createBlockBody(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    afterFuns.memoryOptimizedMap {
                        JsIrBuilder.buildCall(it.symbol).apply {
                            dispatchReceiver = JsIrBuilder.buildGetValue(classVal.symbol)
                        }
                    }
                )
            }

            val finallyLambda = JsIrBuilder.buildFunctionExpression(refType, afterFunction)

            val returnValue = JsIrBuilder.buildCall(
                finally.symbol
            ).apply {
                this.dispatchReceiver = returnStatement.value
                putValueArgument(0, finallyLambda)
            }

            body.statements += JsIrBuilder.buildReturn(
                fn.symbol,
                returnValue,
                fn.returnType
            )

            return
        }

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

    private fun IrClass.instance(): IrExpression {
        return if (kind == ClassKind.OBJECT) {
            JsIrBuilder.buildGetObjectValue(defaultType, symbol)
        } else {
            declarations.asSequence().filterIsInstance<IrConstructor>().first { it.explicitParametersCount == if (isInner) 1 else 0 }
                .let { constructor ->
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
