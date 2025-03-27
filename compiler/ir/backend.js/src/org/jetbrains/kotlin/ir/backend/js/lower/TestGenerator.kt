/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.lower.UpgradeCallableReferences.Companion.selectSAMOverriddenFunction
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.getJsModule
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNameOrKotlinName
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrRichFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.filterIsInstanceAnd
import org.jetbrains.kotlin.utils.findIsInstanceAnd
import org.jetbrains.kotlin.utils.memoryOptimizedMap

fun generateJsTests(context: JsIrBackendContext, moduleFragment: IrModuleFragment) {
    val generator = TestGenerator(context = context)
    moduleFragment.files.forEach {
        val testContainerIfAny = generator.createTestContainer(it)
        if (testContainerIfAny != null) {
            context.testFunsPerFile[it] = testContainerIfAny
        }
    }
}

class TestGenerator(val context: JsCommonBackendContext) {
    fun createTestContainer(irFile: IrFile): IrSimpleFunction? {
        if (irFile.declarations.isEmpty()) return null

        val testContainer = lazy {
            context.irFactory.addFunction(irFile) {
                name = Name.identifier("test fun")
                returnType = context.irBuiltIns.unitType
                origin = JsIrBuilder.SYNTHESIZED_DECLARATION
            }.apply {
                body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, emptyList())
            }
        }

        // Additional copy to prevent ConcurrentModificationException
        ArrayList(irFile.declarations).forEach {
            if (it is IrClass) {
                context.irFactory.stageController.restrictTo(it) {
                    generateTestCalls(it) { testContainer.value }
                }
            }

            // TODO top-level functions
        }

        return if (testContainer.isInitialized()) testContainer.value else null
    }

    private fun IrSimpleFunctionSymbol.createInvocation(
        name: String,
        parentFunction: IrSimpleFunction,
        ignored: Boolean = false,
    ): IrSimpleFunction {
        val body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, emptyList())

        val function = context.irFactory.buildFun {
            this.name = Name.identifier("$name test fun")
            this.returnType = if (this@createInvocation == context.suiteFun!!) context.irBuiltIns.unitType else context.irBuiltIns.anyNType
            this.origin = JsIrBuilder.SYNTHESIZED_DECLARATION
        }

        function.parent = parentFunction
        function.body = body

        val refType = context.symbols.functionN(0).typeWith(function.returnType)
        val testFunReference = IrRichFunctionReferenceImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = refType,
            reflectionTargetSymbol = null,
            overriddenFunctionSymbol = selectSAMOverriddenFunction(refType),
            invokeFunction = function,
            origin = null,
            isRestrictedSuspension = false,
        )

        (parentFunction.body as IrBlockBody).statements += JsIrBuilder.buildCall(this).apply {
            arguments[0] = JsIrBuilder.buildString(context.irBuiltIns.stringType, name)
            arguments[1] = JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, ignored)
            arguments[2] = testFunReference
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
                it.isVisibleFromTests() && it.parameters.size == if (isInner) 1 else 0
            }
        }
    }

    private fun generateCodeForTestMethod(
        testFun: IrSimpleFunction,
        beforeFuns: List<IrSimpleFunction>,
        afterFuns: List<IrSimpleFunction>,
        irClass: IrClass,
        parentFunction: IrSimpleFunction,
    ) {
        val fn = context.testFun!!.createInvocation(testFun.name.asString(), parentFunction, testFun.isIgnored)
        val body = fn.body as IrBlockBody

        val exceptionMessage = when {
            testFun.nonDispatchParameters.isNotEmpty() || !testFun.isEffectivelyVisibleFromTests() ->
                "Test method ${irClass.fqNameWhenAvailable ?: irClass.name}::${testFun.name} should have public or internal visibility, can not have parameters"
            !irClass.canBeInstantiated() ->
                "Test class ${irClass.fqNameWhenAvailable ?: irClass.name} must declare a public or internal constructor with no explicit parameters"
            else -> null
        }

        if (exceptionMessage != null) {
            val irBuilder = context.createIrBuilder(fn.symbol)
            body.statements += irBuilder.irCall(context.irBuiltIns.illegalArgumentExceptionSymbol).apply {
                arguments[0] = irBuilder.irString(exceptionMessage)
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

        val returnType = testFun.returnType as? IrSimpleType
        val promiseSymbol = context.jsPromiseSymbol
        if (promiseSymbol != null && returnType != null && returnType.isPromise) {
            val promiseCastedIfNeeded = if (returnType.classifier == context.jsPromiseSymbol) {
                returnStatement.value
            } else {
                IrTypeOperatorCallImpl(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    type = testFun.returnType,
                    operator = IrTypeOperator.CAST,
                    typeOperand = promiseSymbol.defaultType,
                    argument = returnStatement.value
                )
            }

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

            val refType = context.symbols.functionN(0).typeWith(afterFunction.returnType)
            val finallyLambda = IrRichFunctionReferenceImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                type = refType,
                reflectionTargetSymbol = null,
                overriddenFunctionSymbol = selectSAMOverriddenFunction(refType),
                invokeFunction = afterFunction,
                origin = null,
                isRestrictedSuspension = false,
            )

            val finally = promiseSymbol.owner.declarations
                .findIsInstanceAnd<IrSimpleFunction> { it.name.asString() == "finally" }!!

            val returnValue = JsIrBuilder.buildCall(
                finally.symbol
            ).apply {
                arguments[0] = promiseCastedIfNeeded
                arguments[1] = finallyLambda
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
            declarations.asSequence().filterIsInstance<IrConstructor>().first { it.parameters.size == if (isInner) 1 else 0 }
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

    private val IrSimpleType.isPromise: Boolean
        get() {
            if (this.classifier == context.jsPromiseSymbol) return true
            val klass = classifier.owner as? IrClass ?: return false
            return klass.isExternal && klass.getJsModule() == null && klass.getJsNameOrKotlinName().asString() == "Promise"
        }
}
