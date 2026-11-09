/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

/**
 * Generates visible synthetic accessor functions for symbols that are otherwise inaccessible, for example,
 * when inlining a function that references a private method of a class outside of that class, or generating a class for a lambda
 * expression that uses a `super` qualifier in its body.
 */
abstract class SyntheticAccessorGenerator<Context : LoweringContext, ScopeInfo>(
    protected val context: Context,
) {
    private data class AccessorKey(val parent: IrDeclarationParent, val superQualifierSymbol: IrClassSymbol?)

    protected class AccessorNameBuilder {
        private val nameParts = mutableListOf(ACCESSOR_PREFIX)

        fun contribute(namePart: String) { TODO() }

        fun build(): Name = TODO()
    }

    companion object {
        protected const val ACCESSOR_PREFIX = "access"
        protected const val ACCESSOR_NAME_PARTS_SEPARATOR = "\$"

        protected const val RECEIVER_VALUE_PARAMETER_NAME = "\$this"
        protected const val SETTER_VALUE_PARAMETER_NAME = "<set-?>"

        const val PROPERTY_MARKER = "p"
        const val CONSTRUCTOR_MARKER_PARAMETER_NAME = "constructor_marker"

        private var IrFunction.syntheticAccessors: MutableMap<AccessorKey, IrFunction>? by irAttribute(copyByDefault = false)
        private var IrField.getterSyntheticAccessors: MutableMap<AccessorKey, IrSimpleFunction>? by irAttribute(copyByDefault = false)
        private var IrField.setterSyntheticAccessors: MutableMap<AccessorKey, IrSimpleFunction>? by irAttribute(copyByDefault = false)
    }

    fun getSyntheticFunctionAccessor(expression: IrFunctionAccessExpression, scopeInfo: ScopeInfo): IrFunction { TODO() }

    fun getSyntheticFunctionAccessor(implFunctionSymbol: IrFunctionSymbol, scopeInfo: ScopeInfo): IrFunction { TODO() }

    private fun createAccessor(
        symbol: IrFunctionSymbol,
        scopeInfo: ScopeInfo,
        dispatchReceiverType: IrType?,
        superQualifierSymbol: IrClassSymbol?
    ): IrFunction { TODO() }

    protected abstract fun IrConstructor.makeConstructorAccessor(
        originForConstructorAccessor: IrDeclarationOrigin = IrDeclarationOrigin.SYNTHETIC_ACCESSOR
    ): IrFunction

    protected fun createDelegatingConstructorCall(accessor: IrFunction, targetSymbol: IrConstructorSymbol) =
        IrDelegatingConstructorCallImpl.fromSymbolOwner(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            context.irBuiltIns.unitType,
            targetSymbol, targetSymbol.owner.parentAsClass.typeParameters.size + targetSymbol.owner.typeParameters.size
        ).also {
            copyAllParamsToArgs(it, accessor)
        }

    protected abstract fun accessorModality(parent: IrDeclarationParent): Modality

    private fun IrSimpleFunction.makeSimpleFunctionAccessor(
        superQualifierSymbol: IrClassSymbol?, parent: IrDeclarationParent, scopeInfo: ScopeInfo
    ): IrSimpleFunction { TODO() }

    private fun createSimpleFunctionCall(accessor: IrFunction, targetSymbol: IrSimpleFunctionSymbol, superQualifierSymbol: IrClassSymbol?) =
        IrCallImpl.fromSymbolOwner(
            accessor.startOffset,
            accessor.endOffset,
            accessor.returnType,
            targetSymbol,
            superQualifierSymbol = superQualifierSymbol
        ).also {
            copyAllParamsToArgs(it, accessor)
        }

    fun getSyntheticGetter(expression: IrGetField, scopeInfo: ScopeInfo): IrSimpleFunction { TODO() }

    private fun makeGetterAccessor(
        field: IrField,
        parent: IrDeclarationParent,
        superQualifierSymbol: IrClassSymbol?
    ): IrSimpleFunction = TODO()

    private fun createAccessorBodyForGetter(
        targetField: IrField,
        accessor: IrSimpleFunction,
        superQualifierSymbol: IrClassSymbol?
    ): IrBody { TODO() }

    fun getSyntheticSetter(expression: IrSetField, scopeInfo: ScopeInfo): IrSimpleFunction { TODO() }

    private fun makeSetterAccessor(
        field: IrField,
        parent: IrDeclarationParent,
        superQualifierSymbol: IrClassSymbol?
    ): IrSimpleFunction = TODO()

    private fun createAccessorBodyForSetter(
        targetField: IrField,
        accessor: IrSimpleFunction,
        superQualifierSymbol: IrClassSymbol?
    ): IrBody { TODO() }

    private fun extractFieldAndParent(expression: IrFieldAccessExpression, scopeInfo: ScopeInfo): Pair<IrField, IrDeclarationParent> { TODO() }

    protected fun copyAllParamsToArgs(
        call: IrFunctionAccessExpression,
        syntheticFunction: IrFunction
    ) { TODO() }

    /**
     * In case of Java `protected static`, access could be done from a public inline function in the same package,
     * or a subclass of the Java class. Both cases require an accessor, which we cannot add to a Java class.
     */
    protected abstract fun IrDeclarationWithVisibility.accessorParent(
        parent: IrDeclarationParent,
        scopeInfo: ScopeInfo,
    ): IrDeclarationParent

    protected abstract fun AccessorNameBuilder.buildFunctionName(
        function: IrSimpleFunction,
        superQualifier: IrClassSymbol?,
        scopeInfo: ScopeInfo,
    )

    private fun IrSimpleFunction.accessorName(superQualifier: IrClassSymbol?, scopeInfo: ScopeInfo): Name = TODO()

    protected abstract fun AccessorNameBuilder.buildFieldGetterName(field: IrField, superQualifierSymbol: IrClassSymbol?)
    protected abstract fun AccessorNameBuilder.buildFieldSetterName(field: IrField, superQualifierSymbol: IrClassSymbol?)


    private fun IrField.accessorNameForGetter(superQualifierSymbol: IrClassSymbol?): Name = TODO()

    private fun IrField.accessorNameForSetter(superQualifierSymbol: IrClassSymbol?): Name = TODO()

    /**
     * Produces a call to the synthetic accessor [accessorSymbol] to replace the call expression [oldExpression].
     *
     * Before:
     * ```kotlin
     * class C private constructor(val value: Int) {
     *
     *     private fun privateFun(a: Int): String = a.toString()
     *
     *     internal inline fun foo(x: Int) {
     *         println(privateFun(x))
     *     }
     *
     *     internal inline fun copy(): C = C(value)
     * }
     * ```
     *
     * After:
     * ```kotlin
     * class C private constructor(val value: Int) {
     *
     *     public constructor(
     *         value: Int,
     *         constructor_marker: DefaultConstructorMarker?
     *     ) : this(value)
     *
     *     private fun privateFun(a: Int): String = a.toString()
     *
     *     public static fun access$privateFun($this: C, a: Int): String =
     *         $this.protectedFun(a)
     *
     *     internal inline fun foo(x: Int) {
     *         println(C.access$privateFun(this, x))
     *     }
     *
     *     internal inline fun copy(): C = C(value, null)
     * }
     * ```
     */
    fun modifyFunctionAccessExpression(
        oldExpression: IrFunctionAccessExpression,
        accessorSymbol: IrFunctionSymbol
    ): IrFunctionAccessExpression { TODO() }

    protected open fun capturedTypeParametersOfSyntheticAccessor(declaration: IrDeclaration): List<IrTypeParameter> = TODO()

    private fun IrFunctionSymbol.produceCallToSyntheticFunction(
        oldExpression: IrFunctionAccessExpression
    ): IrCall { TODO() }

    private fun IrFunctionSymbol.produceCallToSyntheticDelegatingConstructor(
        oldExpression: IrFunctionAccessExpression
    ): IrDelegatingConstructorCall { TODO() }

    private fun IrFunctionSymbol.produceCallToSyntheticConstructor(
        oldExpression: IrFunctionAccessExpression
    ): IrFunctionAccessExpression { TODO() }

    abstract fun createAccessorMarkerArgument(): IrConst

    /**
     * Produces a call to the synthetic accessor [accessorSymbol] to replace the field _read_ expression [oldExpression].
     *
     * Before:
     * ```kotlin
     * class C {
     *     protected /*field*/ val myField: Int
     *
     *     internal inline fun foo(): Int = myField + 1
     * }
     * ```
     *
     * After:
     * ```kotlin
     * class C {
     *     protected /*field*/ val myField: Int
     *
     *     public static fun access$getMyField$p($this: C): Int =
     *         $this.myField
     *
     *     internal inline fun foo(): Int =
     *         C.access$getMyField$p(this) + 1
     * }
     * ```
     */
    fun modifyGetterExpression(
        oldExpression: IrGetField,
        accessorSymbol: IrSimpleFunctionSymbol
    ): IrCall { TODO() }

    /**
     * Produces a call to the synthetic accessor [accessorSymbol] to replace the field _write_ expression [oldExpression].
     *
     * Before:
     * ```kotlin
     * class C {
     *     protected var myField: Int = 0
     *
     *     internal inline fun foo(x: Int) {
     *         myField = x
     *     }
     * }
     * ```
     *
     * After:
     * ```kotlin
     * class C {
     *     protected var myField: Int = 0
     *
     *     public static fun access$setMyField$p($this: C, <set-?>: Int) {
     *         $this.myField = <set-?>
     *     }
     *
     *     internal inline fun foo(x: Int) {
     *         access$setMyField$p(this, x)
     *     }
     * }
     * ```
     */
    fun modifySetterExpression(
        oldExpression: IrSetField,
        accessorSymbol: IrSimpleFunctionSymbol
    ): IrCall { TODO() }
}
