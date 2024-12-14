/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.ir.ValueRemapper
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.constructorFactory
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.runUnless
import org.jetbrains.kotlin.utils.memoryOptimizedFilterNot
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.memoryOptimizedPlus
import org.jetbrains.kotlin.utils.newHashMapWithExpectedSize

val ES6_CONSTRUCTOR_REPLACEMENT by IrDeclarationOriginImpl
val ES6_SYNTHETIC_EXPORT_CONSTRUCTOR by IrDeclarationOriginImpl
val ES6_PRIMARY_CONSTRUCTOR_REPLACEMENT by IrDeclarationOriginImpl
val ES6_INIT_FUNCTION by IrDeclarationOriginImpl
val ES6_DELEGATING_CONSTRUCTOR_REPLACEMENT by IrStatementOriginImpl
val ES6_DELEGATING_CONSTRUCTOR_CALL_REPLACEMENT by IrDeclarationOriginImpl

val IrDeclaration.isEs6ConstructorReplacement: Boolean
    get() = origin == ES6_CONSTRUCTOR_REPLACEMENT || origin == ES6_PRIMARY_CONSTRUCTOR_REPLACEMENT

val IrDeclaration.isEs6PrimaryConstructorReplacement: Boolean
    get() = origin == ES6_PRIMARY_CONSTRUCTOR_REPLACEMENT

val IrFunctionAccessExpression.isSyntheticDelegatingReplacement: Boolean
    get() = origin == ES6_DELEGATING_CONSTRUCTOR_REPLACEMENT

val IrDeclaration.isInitFunction: Boolean
    get() = origin == ES6_INIT_FUNCTION

val IrDeclaration.isEs6DelegatingConstructorCallReplacement: Boolean
    get() = origin == ES6_DELEGATING_CONSTRUCTOR_CALL_REPLACEMENT

private val IrClass.constructorPostfix: String
    get() = fqNameWhenAvailable?.asString()?.replace('.', '_') ?: name.toString()

/**
 * Lowers synthetic primary constructor declarations to support ES classes.
 */
class ES6SyntheticPrimaryConstructorLowering(val context: JsIrBackendContext) : DeclarationTransformer {

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (!context.es6mode || declaration !is IrConstructor || declaration.hasStrictSignature(context)) return null

        if (!declaration.isSyntheticPrimaryConstructor) return null // keep existing element
        return listOf(declaration.generateInitFunction())
    }

    /**
     * Generates a static "init" function for this constructor.
     * The function doesn't create a new instance but initializes an existing one.
     *
     * For example, transforms this:
     * ```kotlin
     * package com.example
     *
     * class Foo<T> {
     *   val prop: Int
     *
     *   constructor(arg: Int, $box: Foo<T>?) {
     *       super()
     *       this.prop = arg
     *   }
     * }
     * ```
     * to this:
     * ```kotlin
     * package com.example
     *
     * class Foo<T> {
     *   val prop: Int
     *
     *   private /*static*/ fun <T> Foo<T>.init_com_example_Foo(
     *     <this>: Foo<T>,
     *     arg: Int,
     *     $box: Foo<T>?,
     *   ): Unit {
     *     <this>.prop = arg
     *   }
     * }
     * ```
     */
    private fun IrConstructor.generateInitFunction(): IrSimpleFunction {
        val constructor = this
        val irClass = parentAsClass
        val constructorName = "init_${irClass.constructorPostfix}"
        return context.irFactory.buildFun {
            name = Name.identifier(constructorName)
            returnType = context.irBuiltIns.unitType
            visibility = DescriptorVisibilities.PRIVATE
            modality = Modality.FINAL
            isInline = constructor.isInline
            isExternal = constructor.isExternal
            origin = ES6_INIT_FUNCTION
        }.also { factory ->
            factory.parent = irClass
            factory.copyTypeParametersFrom(irClass)
            factory.annotations = annotations
            factory.extensionReceiverParameter = irClass.thisReceiver?.copyTo(factory)

            factory.body = constructor.body?.deepCopyWithSymbols(factory)?.apply {
                transformChildrenVoid(ValueRemapper(mapOf(irClass.thisReceiver!!.symbol to factory.extensionReceiverParameter!!.symbol)))
            }

            constructorFactory = factory
        }
    }
}

/**
 * Lowers constructor declarations to support ES classes.
 */
class ES6ConstructorLowering(val context: JsIrBackendContext) : DeclarationTransformer {

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (!context.es6mode || declaration !is IrConstructor || declaration.hasStrictSignature(context)) return null

        if (declaration.isSyntheticPrimaryConstructor) return null // keep existing element
        val factoryFunction = declaration.generateCreateFunction()
        return listOfNotNull(factoryFunction, declaration.generateExportedConstructorIfNeed(factoryFunction))
    }

    private fun IrConstructor.generateExportedConstructorIfNeed(factoryFunction: IrSimpleFunction): IrConstructor? {
        return runIf(isExported(context) && isPrimary) {
            apply {
                valueParameters = valueParameters.memoryOptimizedFilterNot { it.isBoxParameter }
                body = (body as? IrBlockBody)?.let {
                    context.irFactory.createBlockBody(it.startOffset, it.endOffset) {
                        val selfReplacedConstructorCall = JsIrBuilder.buildCall(factoryFunction.symbol).apply {
                            valueParameters.forEachIndexed { i, it -> putValueArgument(i, JsIrBuilder.buildGetValue(it.symbol)) }
                            dispatchReceiver = JsIrBuilder.buildCall(context.intrinsics.jsNewTarget)
                        }
                        statements.add(JsIrBuilder.buildReturn(symbol, selfReplacedConstructorCall, returnType))
                    }
                }
                origin = ES6_SYNTHETIC_EXPORT_CONSTRUCTOR
            }
        }
    }

    /**
     * Generates a "create" function to act as a constructor for an ES6 class.
     *
     * Note: although the generated function is not static in the IR,
     * it will become static during code generation.
     *
     * For example, transforms this:
     * ```kotlin
     * package com.example
     *
     * class Foo<T> {
     *   val prop: Int
     *
     *   constructor(arg: Int, $box: Foo<T>?) {
     *       super()
     *       this.prop = arg
     *   }
     * }
     * ```
     *
     * into this:
     * ```kotlin
     * package com.example
     *
     * class Foo<T> {
     *   val prop: Int
     *
     *   fun <T> new_com_example_Foo(arg: Int, $box: Foo<T>?): Foo<T> {
     *     val $this = createThis(this, $box)
     *     $this.prop = arg
     *     return $this
     *   }
     * }
     * ```
     */
    private fun IrConstructor.generateCreateFunction(): IrSimpleFunction {
        val constructor = this
        val irClass = parentAsClass
        val type = irClass.defaultType
        val constructorName = "new_${irClass.constructorPostfix}"

        return context.irFactory.buildFun {
            name = Name.identifier(constructorName)
            returnType = type
            visibility = constructor.visibility
            modality = Modality.FINAL
            isInline = constructor.isInline
            isExternal = constructor.isExternal
            origin = when {
                constructor.isPrimary -> ES6_PRIMARY_CONSTRUCTOR_REPLACEMENT
                else -> ES6_CONSTRUCTOR_REPLACEMENT
            }
        }.also { factory ->
            factory.parent = irClass
            factory.copyTypeParametersFrom(irClass)
            factory.copyParametersFrom(constructor)
            factory.annotations = annotations
            factory.dispatchReceiverParameter = irClass.thisReceiver?.copyTo(factory)

            if (irClass.isExported(context) && constructor.isPrimary) {
                factory.excludeFromExport()
            }

            factory.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                val bodyCopy = constructor.body?.deepCopyWithSymbols(factory) ?: return@createBlockBody
                val self = bodyCopy.replaceSuperCallsAndThisUsages(irClass, factory, constructor)

                statements.addAll(bodyCopy.statements)

                if (self != null) {
                    statements.add(JsIrBuilder.buildReturn(factory.symbol, JsIrBuilder.buildGetValue(self), irClass.defaultType))
                }
            }

            constructorFactory = factory
        }
    }

    private fun IrFunction.generateThisVariable(irClass: IrClass, initializer: IrExpression): IrVariable {
        return JsIrBuilder.buildVar(
            type = irClass.defaultType,
            parent = this,
            name = Namer.SYNTHETIC_RECEIVER_NAME,
            initializer = initializer,
            origin = ES6_DELEGATING_CONSTRUCTOR_CALL_REPLACEMENT
        )
    }

    private fun irAnyArray(elements: List<IrExpression>): IrExpression {
        return JsIrBuilder.buildArray(
            elements,
            context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.anyNType),
            context.irBuiltIns.anyNType,
        )
    }

    private fun IrBody.replaceSuperCallsAndThisUsages(
        irClass: IrClass,
        constructorReplacement: IrSimpleFunction,
        currentConstructor: IrConstructor,
    ): IrValueSymbol? {
        var generatedThisValueSymbol: IrValueSymbol? = null
        var gotLinkageErrorInsteadOfSuperCall = false
        val selfParameterSymbol = irClass.thisReceiver!!.symbol
        val boxParameterSymbol = constructorReplacement.boxParameter

        transformChildrenVoid(object : ValueRemapper(emptyMap()) {
            override val map: MutableMap<IrValueSymbol, IrValueSymbol> = currentConstructor.valueParameters
                .asSequence()
                .zip(constructorReplacement.valueParameters.asSequence())
                .associateTo(newHashMapWithExpectedSize(currentConstructor.valueParameters.size)) { it.first.symbol to it.second.symbol }

            override fun visitReturn(expression: IrReturn): IrExpression {
                return if (expression.returnTargetSymbol == currentConstructor.symbol) {
                    super.visitReturn(
                        JsIrBuilder.buildReturn(
                            constructorReplacement.symbol,
                            JsIrBuilder.buildGetValue(selfParameterSymbol),
                            irClass.defaultType
                        )
                    )
                } else {
                    super.visitReturn(expression)
                }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol == context.irBuiltIns.linkageErrorSymbol) {
                    gotLinkageErrorInsteadOfSuperCall = true
                }
                return super.visitCall(expression)
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                val constructor = expression.symbol.owner

                if (constructor.isSyntheticPrimaryConstructor) {
                    val factoryFunction = constructor.constructorFactory
                    assert(factoryFunction != null && factoryFunction.isInitFunction) { "Expect to have init function replacement" }
                    return JsIrBuilder.buildCall(factoryFunction!!.symbol).apply {
                        copyValueArgumentsFrom(expression, factoryFunction)
                        extensionReceiver = JsIrBuilder.buildGetValue(selfParameterSymbol)
                    }.run { visitCall(this) }
                }

                val boxParameterGetter = boxParameterSymbol?.let { JsIrBuilder.buildGetValue(it.symbol) } ?: context.getVoid()

                val newThisValue = when {
                    constructor.isEffectivelyExternal() ->
                        JsIrBuilder.buildCall(context.intrinsics.jsCreateExternalThisSymbol)
                            .apply {
                                putValueArgument(0, getCurrentConstructorReference(constructorReplacement))
                                putValueArgument(1, expression.symbol.owner.parentAsClass.jsConstructorReference(context))
                                putValueArgument(2, irAnyArray(expression.valueArguments.memoryOptimizedMap { it ?: context.getVoid() }))
                                putValueArgument(3, boxParameterGetter)
                            }
                    constructor.parentAsClass.symbol == context.irBuiltIns.anyClass ->
                        JsIrBuilder.buildCall(context.intrinsics.jsCreateThisSymbol)
                            .apply {
                                putValueArgument(0, getCurrentConstructorReference(constructorReplacement))
                                putValueArgument(1, boxParameterGetter)
                            }
                    else ->
                        JsIrBuilder.buildConstructorCall(
                            expression.symbol,
                            null,
                            expression.typeArguments,
                            ES6_DELEGATING_CONSTRUCTOR_REPLACEMENT
                        ).apply {
                            copyValueArgumentsFrom(expression, constructor)
                        }
                }

                val newThisVariable = constructorReplacement.generateThisVariable(irClass, newThisValue)
                    .also {
                        generatedThisValueSymbol = it.symbol
                        map[selfParameterSymbol] = it.symbol
                    }

                return super.visitComposite(JsIrBuilder.buildComposite(context.irBuiltIns.unitType, listOf(newThisVariable)))
            }
        })

        return generatedThisValueSymbol ?: runUnless<IrValueSymbol?>(gotLinkageErrorInsteadOfSuperCall) {
            irError("Expect to have either super call or partial linkage stub inside constructor") {
                withIrEntry("currentConstructor", currentConstructor)
                withIrEntry("constructorReplacement", constructorReplacement)
            }
        }
    }

    private fun getCurrentConstructorReference(currentFactoryFunction: IrSimpleFunction): IrExpression {
        return JsIrBuilder.buildGetValue(currentFactoryFunction.dispatchReceiverParameter!!.symbol)
    }

    private fun IrDeclaration.excludeFromExport() {
        val jsExportIgnoreClass = context.intrinsics.jsExportIgnoreAnnotationSymbol.owner
        val jsExportIgnoreCtor = jsExportIgnoreClass.primaryConstructor ?: return
        annotations = annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(jsExportIgnoreCtor.symbol)
    }
}
