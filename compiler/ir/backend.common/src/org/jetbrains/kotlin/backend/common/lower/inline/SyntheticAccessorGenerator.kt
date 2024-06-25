/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedString
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap

/**
 * Generates visible synthetic accessor functions for symbols that are otherwise inaccessible, for example,
 * when inlining a function that references a private method of a class outside of that class, or generating a class for a lambda
 * expression that uses a `super` qualifier in its body.
 *
 * TODO: Right now this class is only used in the JVM backend. There is an ongoing effort to also generate synthetic
 *    accessors on KLIB-based backends ([KT-64865](https://youtrack.jetbrains.com/issue/KT-64865)).
 *    Please consider this fact when you want to change its implementation.
 */
open class SyntheticAccessorGenerator<Context : BackendContext>(protected val context: Context) {
    private data class FieldKey(val fieldSymbol: IrFieldSymbol, val parent: IrDeclarationParent, val superQualifierSymbol: IrClassSymbol?)

    private data class FunctionKey(
        val functionSymbol: IrFunctionSymbol,
        val parent: IrDeclarationParent,
        val superQualifierSymbol: IrClassSymbol?
    )

    private val functionMap = ConcurrentHashMap<FunctionKey, IrFunctionSymbol>()
    private val getterMap = ConcurrentHashMap<FieldKey, IrSimpleFunctionSymbol>()
    private val setterMap = ConcurrentHashMap<FieldKey, IrSimpleFunctionSymbol>()

    fun getSyntheticFunctionAccessor(expression: IrFunctionAccessExpression, scopes: List<ScopeWithIr>): IrFunctionSymbol {
        return if (expression is IrCall)
            createAccessor(expression.symbol, scopes, expression.dispatchReceiver?.type, expression.superQualifierSymbol)
        else
            createAccessor(expression.symbol, scopes, null, null)
    }

    fun getSyntheticFunctionAccessor(reference: IrFunctionReference, scopes: List<ScopeWithIr>): IrFunctionSymbol {
        return createAccessor(reference.symbol, scopes, reference.dispatchReceiver?.type, null)
    }

    private fun createAccessor(
        symbol: IrFunctionSymbol,
        scopes: List<ScopeWithIr>,
        dispatchReceiverType: IrType?,
        superQualifierSymbol: IrClassSymbol?
    ): IrFunctionSymbol {
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

        val parent = symbol.owner.accessorParent(dispatchReceiverType?.classOrNull?.owner ?: symbol.owner.parent, scopes)

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
        return functionMap.getOrPut(FunctionKey(symbol, parent, superQualifierSymbol)) {
            when (symbol) {
                is IrConstructorSymbol ->
                    symbol.owner.makeConstructorAccessor().symbol
                is IrSimpleFunctionSymbol ->
                    symbol.owner.makeSimpleFunctionAccessor(superQualifierSymbol, dispatchReceiverType, parent, scopes).symbol
            }
        }
    }

    protected fun IrConstructor.makeConstructorAccessor(
        originForConstructorAccessor: IrDeclarationOrigin = IrDeclarationOrigin.SYNTHETIC_ACCESSOR
    ): IrConstructor {
        val source = this

        return factory.buildConstructor {
            origin = originForConstructorAccessor
            name = source.name
            visibility = DescriptorVisibilities.PUBLIC
        }.also { accessor ->
            accessor.parent = source.parent

            accessor.copyTypeParametersFrom(source, IrDeclarationOrigin.SYNTHETIC_ACCESSOR)
            accessor.copyValueParametersToStatic(source, IrDeclarationOrigin.SYNTHETIC_ACCESSOR)
            if (source.constructedClass.modality == Modality.SEALED) {
                for (accessorValueParameter in accessor.valueParameters) {
                    accessorValueParameter.annotations = emptyList()
                }
            }

            accessor.returnType = source.returnType.remapTypeParameters(source, accessor)

            accessor.addValueParameter(
                "constructor_marker".synthesizedString,
                context.ir.symbols.defaultConstructorMarker.defaultType.makeNullable(),
                IrDeclarationOrigin.DEFAULT_CONSTRUCTOR_MARKER,
            )

            accessor.body = context.irFactory.createBlockBody(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                listOf(createConstructorCall(accessor, source.symbol))
            )
        }
    }

    private fun createConstructorCall(accessor: IrConstructor, targetSymbol: IrConstructorSymbol) =
        IrDelegatingConstructorCallImpl.fromSymbolOwner(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            context.irBuiltIns.unitType,
            targetSymbol, targetSymbol.owner.parentAsClass.typeParameters.size + targetSymbol.owner.typeParameters.size
        ).also {
            copyAllParamsToArgs(it, accessor)
        }

    protected open fun accessorModality(parent: IrDeclarationParent): Modality = Modality.FINAL

    private fun IrSimpleFunction.makeSimpleFunctionAccessor(
        superQualifierSymbol: IrClassSymbol?, dispatchReceiverType: IrType?, parent: IrDeclarationParent, scopes: List<ScopeWithIr>
    ): IrSimpleFunction {
        val source = this

        return factory.buildFun {
            startOffset = parent.startOffset
            endOffset = parent.startOffset
            origin = IrDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = source.accessorName(superQualifierSymbol, scopes)
            visibility = DescriptorVisibilities.PUBLIC
            modality = accessorModality(parent)
            isSuspend = source.isSuspend // synthetic accessors of suspend functions are handled in codegen
        }.also { accessor ->
            accessor.parent = parent
            accessor.copyAttributes(source)
            accessor.copyTypeParametersFrom(source, IrDeclarationOrigin.SYNTHETIC_ACCESSOR)
            accessor.copyValueParametersToStatic(source, IrDeclarationOrigin.SYNTHETIC_ACCESSOR, dispatchReceiverType)
            accessor.returnType = source.returnType.remapTypeParameters(source, accessor)

            accessor.body = context.irFactory.createExpressionBody(
                accessor.startOffset, accessor.startOffset,
                createSimpleFunctionCall(accessor, source.symbol, superQualifierSymbol)
            )
        }
    }

    private fun createSimpleFunctionCall(accessor: IrFunction, targetSymbol: IrSimpleFunctionSymbol, superQualifierSymbol: IrClassSymbol?) =
        IrCallImpl.fromSymbolOwner(
            accessor.startOffset,
            accessor.endOffset,
            accessor.returnType,
            targetSymbol, targetSymbol.owner.typeParameters.size,
            superQualifierSymbol = superQualifierSymbol
        ).also {
            copyAllParamsToArgs(it, accessor)
        }

    fun getSyntheticGetter(expression: IrGetField, scopes: List<ScopeWithIr>): IrSimpleFunctionSymbol {
        val dispatchReceiverType = expression.receiver?.type
        val dispatchReceiverClassSymbol = dispatchReceiverType?.classifierOrNull as? IrClassSymbol
        val symbol = expression.symbol
        val parent = symbol.owner.accessorParent(dispatchReceiverClassSymbol?.owner ?: symbol.owner.parent, scopes) as IrClass
        return getterMap.getOrPut(FieldKey(symbol, parent, expression.superQualifierSymbol)) {
            makeGetterAccessorSymbol(symbol, parent, expression.superQualifierSymbol)
        }
    }

    private fun makeGetterAccessorSymbol(
        fieldSymbol: IrFieldSymbol,
        parent: IrClass,
        superQualifierSymbol: IrClassSymbol?
    ): IrSimpleFunctionSymbol =
        context.irFactory.buildFun {
            startOffset = parent.startOffset
            endOffset = parent.startOffset
            origin = IrDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = fieldSymbol.owner.accessorNameForGetter(superQualifierSymbol)
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            returnType = fieldSymbol.owner.type
        }.also { accessor ->
            accessor.parent = parent

            if (!fieldSymbol.owner.isStatic) {
                // Accessors are always to one's own fields.
                accessor.addValueParameter(
                    "\$this", parent.defaultType, IrDeclarationOrigin.SYNTHETIC_ACCESSOR
                )
            }

            accessor.body = createAccessorBodyForGetter(fieldSymbol.owner, accessor, superQualifierSymbol)
        }.symbol

    private fun createAccessorBodyForGetter(
        targetField: IrField,
        accessor: IrSimpleFunction,
        superQualifierSymbol: IrClassSymbol?
    ): IrBody {
        val maybeDispatchReceiver =
            if (targetField.isStatic) null
            else IrGetValueImpl(accessor.startOffset, accessor.endOffset, accessor.valueParameters[0].symbol)
        return context.irFactory.createExpressionBody(
            accessor.startOffset, accessor.endOffset,
            IrGetFieldImpl(
                accessor.startOffset, accessor.endOffset,
                targetField.symbol,
                targetField.type,
                maybeDispatchReceiver,
                superQualifierSymbol = superQualifierSymbol
            )
        )
    }

    fun getSyntheticSetter(expression: IrSetField, scopes: List<ScopeWithIr>): IrSimpleFunctionSymbol {
        val dispatchReceiverType = expression.receiver?.type
        val dispatchReceiverClassSymbol = dispatchReceiverType?.classifierOrNull as? IrClassSymbol
        val symbol = expression.symbol
        val parent = symbol.owner.accessorParent(dispatchReceiverClassSymbol?.owner ?: symbol.owner.parent, scopes) as IrClass
        return setterMap.getOrPut(FieldKey(symbol, parent, expression.superQualifierSymbol)) {
            makeSetterAccessorSymbol(symbol, parent, expression.superQualifierSymbol)
        }
    }

    private fun makeSetterAccessorSymbol(
        fieldSymbol: IrFieldSymbol,
        parent: IrClass,
        superQualifierSymbol: IrClassSymbol?
    ): IrSimpleFunctionSymbol =
        context.irFactory.buildFun {
            startOffset = parent.startOffset
            endOffset = parent.startOffset
            origin = IrDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = fieldSymbol.owner.accessorNameForSetter(superQualifierSymbol)
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            returnType = context.irBuiltIns.unitType
        }.also { accessor ->
            accessor.parent = parent

            if (!fieldSymbol.owner.isStatic) {
                // Accessors are always to one's own fields.
                accessor.addValueParameter(
                    "\$this", parent.defaultType, IrDeclarationOrigin.SYNTHETIC_ACCESSOR
                )
            }

            accessor.addValueParameter("<set-?>", fieldSymbol.owner.type, IrDeclarationOrigin.SYNTHETIC_ACCESSOR)

            accessor.body = createAccessorBodyForSetter(fieldSymbol.owner, accessor, superQualifierSymbol)
        }.symbol

    private fun createAccessorBodyForSetter(
        targetField: IrField,
        accessor: IrSimpleFunction,
        superQualifierSymbol: IrClassSymbol?
    ): IrBody {
        val maybeDispatchReceiver =
            if (targetField.isStatic) null
            else IrGetValueImpl(accessor.startOffset, accessor.endOffset, accessor.valueParameters[0].symbol)
        val value = IrGetValueImpl(
            accessor.startOffset, accessor.endOffset,
            accessor.valueParameters[if (targetField.isStatic) 0 else 1].symbol
        )
        return context.irFactory.createExpressionBody(
            accessor.startOffset, accessor.endOffset,
            IrSetFieldImpl(
                accessor.startOffset, accessor.endOffset,
                targetField.symbol,
                maybeDispatchReceiver,
                value,
                context.irBuiltIns.unitType,
                superQualifierSymbol = superQualifierSymbol
            )
        )
    }

    private fun copyAllParamsToArgs(
        call: IrFunctionAccessExpression,
        syntheticFunction: IrFunction
    ) {
        var typeArgumentOffset = 0
        if (syntheticFunction is IrConstructor) {
            call.passTypeArgumentsFrom(syntheticFunction.parentAsClass)
            typeArgumentOffset = syntheticFunction.parentAsClass.typeParameters.size
        }
        call.passTypeArgumentsFrom(syntheticFunction, offset = typeArgumentOffset)

        var offset = 0
        val delegateTo = call.symbol.owner
        delegateTo.dispatchReceiverParameter?.let {
            call.dispatchReceiver =
                IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, syntheticFunction.valueParameters[offset++].symbol)
        }

        delegateTo.extensionReceiverParameter?.let {
            call.extensionReceiver =
                IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, syntheticFunction.valueParameters[offset++].symbol)
        }

        delegateTo.valueParameters.forEachIndexed { i, _ ->
            call.putValueArgument(
                i,
                IrGetValueImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    syntheticFunction.valueParameters[i + offset].symbol
                )
            )
        }
    }

    /**
     * In case of Java `protected static`, access could be done from a public inline function in the same package,
     * or a subclass of the Java class. Both cases require an accessor, which we cannot add to a Java class.
     */
    protected open fun IrDeclarationWithVisibility.accessorParent(parent: IrDeclarationParent, scopes: List<ScopeWithIr>) = parent

    protected open fun mapFunctionName(function: IrSimpleFunction): String = function.name.asString()

    protected open fun functionAccessorSuffix(
        function: IrSimpleFunction,
        superQualifier: IrClassSymbol?,
        scopes: List<ScopeWithIr>,
    ): String = function.run {
        when {
            // Accessors for top level functions never need a suffix.
            isTopLevel -> ""

            // Accessor for _s_uper-qualified call
            superQualifier != null -> "\$s" + superQualifier.owner.syntheticAccessorToSuperSuffix()

            // Access to protected members that need an accessor must be because they are inherited,
            // hence accessed on a _s_upertype. If what is accessed is static, we can point to different
            // parts of the inheritance hierarchy and need to distinguish with a suffix.
            isStatic && visibility.isProtected -> "\$s" + parentAsClass.syntheticAccessorToSuperSuffix()

            else -> ""
        }
    }

    private fun IrSimpleFunction.accessorName(superQualifier: IrClassSymbol?, scopes: List<ScopeWithIr>): Name {
        return Name.identifier("access\$${mapFunctionName(this)}${functionAccessorSuffix(this, superQualifier, scopes)}")
    }

    protected open fun fieldGetterName(field: IrField): String = "<get-${field.name}>"

    private fun IrField.accessorNameForGetter(superQualifierSymbol: IrClassSymbol?): Name {
        val getterName = fieldGetterName(this)
        return Name.identifier("access\$$getterName\$${fieldAccessorSuffix(this, superQualifierSymbol)}")
    }

    protected open fun fieldSetterName(field: IrField): String = "<set-${field.name}>"

    private fun IrField.accessorNameForSetter(superQualifierSymbol: IrClassSymbol?): Name {
        val setterName = fieldSetterName(this)
        return Name.identifier("access\$$setterName\$${fieldAccessorSuffix(this, superQualifierSymbol)}")
    }

    /**
     * For both _reading_ and _writing_ field accessors, the suffix that includes some of [field]'s important properties.
     */
    protected open fun fieldAccessorSuffix(field: IrField, superQualifierSymbol: IrClassSymbol?): String = field.run {
        if (superQualifierSymbol != null) {
            return "p\$s${superQualifierSymbol.owner.syntheticAccessorToSuperSuffix()}"
        }

        // Accesses to static protected fields that need an accessor must be due to being inherited, hence accessed on a
        // _s_upertype. If the field is static, the super class the access is on can be different, and therefore
        // we generate a suffix to distinguish access to field with different receiver types in the super hierarchy.
        return "p" + if (isStatic && visibility.isProtected) "\$s" + parentAsClass.syntheticAccessorToSuperSuffix() else ""
    }

    private fun IrClass.syntheticAccessorToSuperSuffix(): String =
        // TODO: change this to `fqNameUnsafe.asString().replace(".", "_")` as soon as we're ready to break compatibility with pre-KT-21178 code
        name.asString().hashCode().toString()

    protected open val DescriptorVisibility.isProtected
        get() = this == DescriptorVisibilities.PROTECTED

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
        val newExpression = when (oldExpression) {
            is IrCall -> IrCallImpl.fromSymbolOwner(
                oldExpression.startOffset, oldExpression.endOffset,
                oldExpression.type,
                accessorSymbol as IrSimpleFunctionSymbol, oldExpression.typeArgumentsCount,
                origin = oldExpression.origin
            )
            is IrDelegatingConstructorCall -> IrDelegatingConstructorCallImpl.fromSymbolOwner(
                oldExpression.startOffset, oldExpression.endOffset,
                context.irBuiltIns.unitType,
                accessorSymbol as IrConstructorSymbol, oldExpression.typeArgumentsCount
            )
            is IrConstructorCall ->
                IrConstructorCallImpl.fromSymbolOwner(
                    oldExpression.startOffset, oldExpression.endOffset,
                    oldExpression.type,
                    accessorSymbol as IrConstructorSymbol
                )
            is IrEnumConstructorCall -> compilationException(
                "Generating synthetic accessors for IrEnumConstructorCall is not supported",
                oldExpression,
            )
        }
        newExpression.copyTypeArgumentsFrom(oldExpression)
        val receiverAndArgs = oldExpression.receiverAndArgs()
        receiverAndArgs.forEachIndexed { i, irExpression ->
            newExpression.putValueArgument(i, irExpression)
        }
        if (accessorSymbol is IrConstructorSymbol) {
            newExpression.putValueArgument(receiverAndArgs.size, createAccessorMarkerArgument())
        }
        return newExpression
    }

    fun createAccessorMarkerArgument() =
        IrConstImpl.constNull(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.ir.symbols.defaultConstructorMarker.defaultType.makeNullable())
}