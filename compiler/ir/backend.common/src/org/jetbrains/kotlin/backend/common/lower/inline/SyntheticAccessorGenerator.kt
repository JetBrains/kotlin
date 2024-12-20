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

        fun contribute(namePart: String) {
            nameParts += namePart
        }

        fun build(): Name = Name.identifier(nameParts.joinToString(ACCESSOR_NAME_PARTS_SEPARATOR))
    }

    companion object {
        protected const val ACCESSOR_PREFIX = "access"
        protected const val ACCESSOR_NAME_PARTS_SEPARATOR = "\$"

        protected const val RECEIVER_VALUE_PARAMETER_NAME = "\$this"
        protected const val SETTER_VALUE_PARAMETER_NAME = "<set-?>"

        const val PROPERTY_MARKER = "p"

        private var IrFunction.syntheticAccessors: MutableMap<AccessorKey, IrFunction>? by irAttribute(followAttributeOwner = false)
        private var IrField.getterSyntheticAccessors: MutableMap<AccessorKey, IrSimpleFunction>? by irAttribute(followAttributeOwner = false)
        private var IrField.setterSyntheticAccessors: MutableMap<AccessorKey, IrSimpleFunction>? by irAttribute(followAttributeOwner = false)
    }

    fun getSyntheticFunctionAccessor(expression: IrFunctionAccessExpression, scopeInfo: ScopeInfo): IrFunction {
        return if (expression is IrCall)
            createAccessor(expression.symbol, scopeInfo, expression.dispatchReceiver?.type, expression.superQualifierSymbol)
        else
            createAccessor(expression.symbol, scopeInfo, null, null)
    }

    fun getSyntheticFunctionAccessor(reference: IrFunctionReference, scopeInfo: ScopeInfo): IrFunction {
        return createAccessor(reference.symbol, scopeInfo, reference.dispatchReceiver?.type, null)
    }

    private fun createAccessor(
        symbol: IrFunctionSymbol,
        scopeInfo: ScopeInfo,
        dispatchReceiverType: IrType?,
        superQualifierSymbol: IrClassSymbol?
    ): IrFunction {
        // Find the right container to insert the accessor. Simply put, when we call a function on a class A,
        // we also need to put its accessor into A. However, due to the way that calls are implemented in the
        // IR we generally need to look at the type of the dispatchReceiver *argument* in order to find the
        // correct class. Consider the following code:
        //
        //     fun run(f : () -> Int): Int = f()
        //
        //     open class A {
        //         private fun f() = 0
        //         fun g() = run { this.f() }
        //     }
        //
        //     class B : A {
        //         override fun g() = 1
        //         fun h() = run { super.g() }
        //     }
        //
        // We have calls to the private methods A.f from a generated Lambda subclass for the argument to `run`
        // in class A and a super call to A.g from a generated Lambda subclass in class B.
        //
        // In the first case, we need to produce an accessor in class A to access the private member of A.
        // Both the parent of the function f and the type of the dispatch receiver point to the correct class.
        // In the second case we need to call A.g from within class B, since this is the only way to invoke
        // a method of a superclass on the JVM. However, the IR for the call to super.g points directly to the
        // function g in class A. Confusingly, the `superQualifier` on this call also points to class A.
        // The only way to compute the actual enclosing class for the call is by looking at the type of the
        // dispatch receiver argument, which points to B.
        //
        // Beyond this, there can be accessors that are needed because other lowerings produce code calling
        // private methods (e.g., local functions for lambdas are private and called from generated
        // SAM wrapper classes). In this case we rely on the parent field of the called function.
        //
        // Finally, we need to produce accessors for calls to protected static methods coming from Java,
        // which we put in the closest enclosing class which has access to the method in question.
        val function = symbol.owner
        val parent = function.accessorParent(dispatchReceiverType?.classOrNull?.owner ?: function.parent, scopeInfo)
        if (parent !is IrDeclarationContainer) compilationException("The accessor parent must be IrDeclarationContainer", parent)

        // The key in the cache/map needs to be BOTH the symbol of the function being accessed AND the parent
        // of the accessor. Going from the above example, if we have another class C similar to B:
        //
        //     class C : A {
        //         override fun g() = 2
        //         fun i() = run { super.g() }
        //     }
        //
        // For the call to super.g in function i, the accessor to A.g must be produced in C. Therefore, we
        // cannot use the function symbol (A.g in the example) by itself as the key since there should be
        // one accessor per dispatch receiver (i.e., parent of the accessor).
        val functionMap = function.syntheticAccessors ?: hashMapOf<AccessorKey, IrFunction>().also { function.syntheticAccessors = it }
        return functionMap.getOrPut(AccessorKey(parent, superQualifierSymbol)) {
            when (function) {
                is IrConstructor ->
                    function.makeConstructorAccessor()
                is IrSimpleFunction ->
                    function.makeSimpleFunctionAccessor(superQualifierSymbol, dispatchReceiverType, parent, scopeInfo)
            }
        }
    }

    protected abstract fun IrConstructor.makeConstructorAccessor(
        originForConstructorAccessor: IrDeclarationOrigin = IrDeclarationOrigin.SYNTHETIC_ACCESSOR
    ): IrFunction

    protected abstract fun accessorModality(parent: IrDeclarationParent): Modality

    private fun IrSimpleFunction.makeSimpleFunctionAccessor(
        superQualifierSymbol: IrClassSymbol?, dispatchReceiverType: IrType?, parent: IrDeclarationParent, scopeInfo: ScopeInfo
    ): IrSimpleFunction {
        val source = this

        return factory.buildFun {
            startOffset = parent.startOffset
            endOffset = parent.startOffset
            origin = IrDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = source.accessorName(superQualifierSymbol, scopeInfo)
            visibility = DescriptorVisibilities.PUBLIC
            modality = accessorModality(parent)
            isSuspend = source.isSuspend // synthetic accessors of suspend functions are handled in codegen
        }.also { accessor ->
            accessor.parent = parent
            accessor.copyAttributes(source)
            accessor.copyTypeParametersFrom(source, IrDeclarationOrigin.SYNTHETIC_ACCESSOR)
            accessor.copyValueParametersToStatic(source, IrDeclarationOrigin.SYNTHETIC_ACCESSOR, dispatchReceiverType)
            accessor.returnType = source.returnType.remapTypeParameters(source, accessor)

            accessor.body = context.irFactory.createBlockBody(
                accessor.startOffset, accessor.startOffset,
                listOf(
                    IrReturnImpl(
                        accessor.startOffset, accessor.endOffset,
                        context.irBuiltIns.nothingType,
                        accessor.symbol,
                        createSimpleFunctionCall(accessor, source.symbol, superQualifierSymbol)
                    )
                )
            )
        }
    }

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

    fun getSyntheticGetter(expression: IrGetField, scopeInfo: ScopeInfo): IrSimpleFunction {
        val (field, parent) = extractFieldAndParent(expression, scopeInfo)
        val getterMap =
            field.getterSyntheticAccessors ?: hashMapOf<AccessorKey, IrSimpleFunction>().also { field.getterSyntheticAccessors = it }
        return getterMap.getOrPut(AccessorKey(parent, expression.superQualifierSymbol)) {
            makeGetterAccessor(field, parent, expression.superQualifierSymbol)
        }
    }

    private fun makeGetterAccessor(
        field: IrField,
        parent: IrDeclarationParent,
        superQualifierSymbol: IrClassSymbol?
    ): IrSimpleFunction =
        context.irFactory.buildFun {
            startOffset = parent.startOffset
            endOffset = parent.startOffset
            origin = IrDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = field.accessorNameForGetter(superQualifierSymbol)
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            returnType = field.type
        }.also { accessor ->
            accessor.parent = parent

            if (!field.isStatic) {
                // Accessors are always to one's own fields.
                accessor.addValueParameter(
                    RECEIVER_VALUE_PARAMETER_NAME, (parent as IrClass).defaultType, IrDeclarationOrigin.SYNTHETIC_ACCESSOR
                )
            }

            accessor.body = createAccessorBodyForGetter(field, accessor, superQualifierSymbol)
        }

    private fun createAccessorBodyForGetter(
        targetField: IrField,
        accessor: IrSimpleFunction,
        superQualifierSymbol: IrClassSymbol?
    ): IrBody {
        val maybeDispatchReceiver =
            if (targetField.isStatic) null
            else IrGetValueImpl(accessor.startOffset, accessor.endOffset, accessor.parameters[0].symbol)
        val irGetField = IrGetFieldImpl(
            accessor.startOffset, accessor.endOffset,
            targetField.symbol,
            targetField.type,
            maybeDispatchReceiver,
            superQualifierSymbol = superQualifierSymbol
        )
        return context.irFactory.createBlockBody(
            accessor.startOffset, accessor.endOffset,
            listOf(
                IrReturnImpl(
                    accessor.startOffset, accessor.endOffset, context.irBuiltIns.nothingType, accessor.symbol, irGetField
                )
            )
        )
    }

    fun getSyntheticSetter(expression: IrSetField, scopeInfo: ScopeInfo): IrSimpleFunction {
        val (field, parent) = extractFieldAndParent(expression, scopeInfo)
        val setterMap =
            field.setterSyntheticAccessors ?: hashMapOf<AccessorKey, IrSimpleFunction>().also { field.setterSyntheticAccessors = it }
        return setterMap.getOrPut(AccessorKey(parent, expression.superQualifierSymbol)) {
            makeSetterAccessor(field, parent, expression.superQualifierSymbol)
        }
    }

    private fun makeSetterAccessor(
        field: IrField,
        parent: IrDeclarationParent,
        superQualifierSymbol: IrClassSymbol?
    ): IrSimpleFunction =
        context.irFactory.buildFun {
            startOffset = parent.startOffset
            endOffset = parent.startOffset
            origin = IrDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = field.accessorNameForSetter(superQualifierSymbol)
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            returnType = context.irBuiltIns.unitType
        }.also { accessor ->
            accessor.parent = parent

            if (!field.isStatic) {
                // Accessors are always to one's own fields.
                accessor.addValueParameter(
                    RECEIVER_VALUE_PARAMETER_NAME, (parent as IrClass).defaultType, IrDeclarationOrigin.SYNTHETIC_ACCESSOR
                )
            }

            accessor.addValueParameter(SETTER_VALUE_PARAMETER_NAME, field.type, IrDeclarationOrigin.SYNTHETIC_ACCESSOR)

            accessor.body = createAccessorBodyForSetter(field, accessor, superQualifierSymbol)
        }

    private fun createAccessorBodyForSetter(
        targetField: IrField,
        accessor: IrSimpleFunction,
        superQualifierSymbol: IrClassSymbol?
    ): IrBody {
        val maybeDispatchReceiver =
            if (targetField.isStatic) null
            else IrGetValueImpl(accessor.startOffset, accessor.endOffset, accessor.parameters[0].symbol)
        val value = IrGetValueImpl(
            accessor.startOffset, accessor.endOffset,
            accessor.parameters[if (targetField.isStatic) 0 else 1].symbol
        )
        val irSetField = IrSetFieldImpl(
            accessor.startOffset, accessor.endOffset,
            targetField.symbol,
            maybeDispatchReceiver,
            value,
            context.irBuiltIns.unitType,
            superQualifierSymbol = superQualifierSymbol
        )
        return context.irFactory.createBlockBody(
            accessor.startOffset, accessor.endOffset,
            listOf(
                IrReturnImpl(
                    accessor.startOffset, accessor.endOffset, context.irBuiltIns.nothingType, accessor.symbol, irSetField
                )
            )
        )
    }

    private fun extractFieldAndParent(expression: IrFieldAccessExpression, scopeInfo: ScopeInfo): Pair<IrField, IrDeclarationParent> {
        val dispatchReceiverClassSymbol = expression.receiver?.type?.classifierOrNull as? IrClassSymbol
        val field = expression.symbol.owner
        val parent = field.accessorParent(dispatchReceiverClassSymbol?.owner ?: field.parent, scopeInfo)

        return field to parent
    }

    protected fun copyAllParamsToArgs(
        call: IrFunctionAccessExpression,
        syntheticFunction: IrFunction
    ) {
        var typeArgumentOffset = 0
        if (syntheticFunction is IrConstructor) {
            call.passTypeArgumentsFrom(syntheticFunction.parentAsClass)
            typeArgumentOffset = syntheticFunction.parentAsClass.typeParameters.size
        }
        call.passTypeArgumentsFrom(syntheticFunction, offset = typeArgumentOffset)

        val delegateTo = call.symbol.owner

        delegateTo.parameters.indices.forEach { index ->
            call.arguments[index] = IrGetValueImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                symbol = syntheticFunction.parameters[index].symbol
            )
        }
    }

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

    private fun IrSimpleFunction.accessorName(superQualifier: IrClassSymbol?, scopeInfo: ScopeInfo): Name =
        AccessorNameBuilder().apply { buildFunctionName(this@accessorName, superQualifier, scopeInfo) }.build()

    protected abstract fun AccessorNameBuilder.buildFieldGetterName(field: IrField, superQualifierSymbol: IrClassSymbol?)
    protected abstract fun AccessorNameBuilder.buildFieldSetterName(field: IrField, superQualifierSymbol: IrClassSymbol?)


    private fun IrField.accessorNameForGetter(superQualifierSymbol: IrClassSymbol?): Name =
        AccessorNameBuilder().apply { buildFieldGetterName(this@accessorNameForGetter, superQualifierSymbol) }.build()

    private fun IrField.accessorNameForSetter(superQualifierSymbol: IrClassSymbol?): Name =
        AccessorNameBuilder().apply { buildFieldSetterName(this@accessorNameForSetter, superQualifierSymbol) }.build()

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
    ): IrFunctionAccessExpression {
        val newExpression = when {
            oldExpression is IrDelegatingConstructorCall -> accessorSymbol.produceCallToSyntheticDelegatingConstructor(oldExpression)
            oldExpression is IrEnumConstructorCall -> compilationException(
                "Generating synthetic accessors for IrEnumConstructorCall is not supported",
                oldExpression,
            )
            accessorSymbol is IrConstructorSymbol -> accessorSymbol.produceCallToSyntheticConstructor(oldExpression)
            else -> accessorSymbol.produceCallToSyntheticFunction(oldExpression)
        }
        newExpression.copyTypeArgumentsFrom(oldExpression)
        val newExpressionArguments = if (accessorSymbol is IrConstructorSymbol) {
            oldExpression.receiverAndArgs() + createAccessorMarkerArgument()
        } else {
            oldExpression.receiverAndArgs()
        }
        newExpression.arguments.assignFrom(newExpressionArguments)
        return newExpression
    }

    private fun IrFunctionSymbol.produceCallToSyntheticFunction(
        oldExpression: IrFunctionAccessExpression
    ): IrCall {
        return IrCallImpl.fromSymbolOwner(
            oldExpression.startOffset, oldExpression.endOffset,
            oldExpression.type,
            this as IrSimpleFunctionSymbol,
            origin = oldExpression.origin
        )
    }

    private fun IrFunctionSymbol.produceCallToSyntheticDelegatingConstructor(
        oldExpression: IrFunctionAccessExpression
    ): IrDelegatingConstructorCall {
        return IrDelegatingConstructorCallImpl.fromSymbolOwner(
            oldExpression.startOffset, oldExpression.endOffset,
            context.irBuiltIns.unitType,
            this as IrConstructorSymbol
        )
    }

    private fun IrFunctionSymbol.produceCallToSyntheticConstructor(
        oldExpression: IrFunctionAccessExpression
    ): IrFunctionAccessExpression {
        return IrConstructorCallImpl.fromSymbolOwner(
            oldExpression.startOffset, oldExpression.endOffset,
            oldExpression.type,
            this as IrConstructorSymbol
        )
    }

    fun createAccessorMarkerArgument() =
        IrConstImpl.constNull(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.ir.symbols.defaultConstructorMarker.defaultType.makeNullable())

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
    ): IrCall {
        val call = IrCallImpl(
            oldExpression.startOffset, oldExpression.endOffset,
            oldExpression.type,
            accessorSymbol, 0,
            oldExpression.origin
        )
        oldExpression.receiver?.let {
            call.arguments[0] = oldExpression.receiver
        }
        return call
    }

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
    ): IrCall {
        val call = IrCallImpl(
            oldExpression.startOffset, oldExpression.endOffset,
            oldExpression.type,
            accessorSymbol, 0,
            oldExpression.origin
        )
        if (oldExpression.receiver != null) {
            call.arguments[0] = oldExpression.receiver
            call.arguments[1] = oldExpression.value
        } else {
            call.arguments[0] = oldExpression.value
        }
        return call
    }
}
