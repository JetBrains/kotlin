/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.inline
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.fileParentOrNull
import org.jetbrains.kotlin.backend.jvm.lower.JvmPropertiesLowering.Companion.createSyntheticMethodForPropertyDelegate
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name

/**
 * Optimizes `val x by ::y`: instead of constructing a `KProperty` instance and calling `getValue`/`setValue` operators, generates calls
 * to the getter/setter of the referenced property directly. If the property reference has a bound receiver which is non-trivial
 * (its computation might lead to side effects), we compute the receiver once and store it in a field.
 *
 * Also, generates a `$delegate` method that returns the delegate anyway. This method is supposed to only be used from kotlin-reflect
 * ([kotlin.reflect.KProperty0.getDelegate]).
 *
 * For example:
 *
 *     var x by f()::y
 *
 * becomes
 *
 *     var x
 *         field = f()
 *         get() = field.y()
 *         set(value) { field.y = value }
 *     fun getX$delegate() = x$field::y
 */
internal class PropertyReferenceDelegationLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transform(PropertyReferenceDelegationTransformer(context), null)
    }
}

private class PropertyReferenceDelegationTransformer(val context: JvmBackendContext) : IrElementTransformerVoid() {

    // Some receivers don't need to be stored in fields and can be reevaluated every time an accessor is called:
    private fun IrExpression?.canInline(visibleScopes: Set<IrDeclarationParent>): Boolean = when (this) {
        null -> true
        is IrGetValue -> {
            // Reads of immutable variables are stable, but value parameters of the constructor are not in scope:
            val value = symbol.owner
            !(value is IrVariable && value.isVar) && value.parent in visibleScopes
        }
        is IrGetField -> {
            // Reads of final fields of stable values are stable, but fields in other files can become non-final:
            val field = symbol.owner
            field.isFinal && field.fileParentOrNull.let { it != null && it in visibleScopes }
                    && receiver.canInline(visibleScopes)
        }
        is IrCall -> {
            // Same applies to reads of properties with default getters, but non-final properties may be overridden by `var`s:
            val callee = symbol.owner
            callee.isFinalDefaultValGetter && callee.fileParentOrNull.let { it != null && it in visibleScopes }
                    && arguments.all { it.canInline(visibleScopes) }
        }
        else -> {
            // Constants and singleton object accesses are always stable:
            isTrivial()
        }
    }

    private val IrSimpleFunction.isFinalDefaultValGetter: Boolean
        get() = origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR &&
                correspondingPropertySymbol?.let { it.owner.getter == this && it.owner.setter == null } == true &&
                modality == Modality.FINAL

    override fun visitClass(declaration: IrClass): IrStatement {
        declaration.transformChildren(this, null)
        declaration.transformDeclarationsFlat {
            (it as? IrProperty)?.transform()
        }
        return declaration
    }

    fun DeclarationIrBuilder.createGetterBody(
        getter: IrSimpleFunction,
        delegateReference: IrRichPropertyReference,
        receiverProvider: IrBuilder.() -> IrExpression?,
    ): IrBody {
        val constInitializer = delegateReference.constInitializer
        return if (constInitializer != null) {
            // Const initializer is a special case, see KT-63580
            irExprBody(constInitializer)
        } else {
            irExprBody(irBlock {
                +delegateReference.getterFunction.inline(
                    getter,
                    createAccessorArgumentsList(getter, delegateReference.getterFunction, isGetter = true, receiverProvider)
                )
            })
        }
    }

    fun DeclarationIrBuilder.createSetterBody(
        setter: IrSimpleFunction,
        delegateReference: IrRichPropertyReference,
        receiverProvider: IrBuilder.() -> IrExpression?,
    ): IrBody {
        val delegateSetter = delegateReference.setterFunction ?: error("delegate was expected to have a setter")
        return irExprBody(irBlock {
            +delegateSetter.inline(
                setter,
                createAccessorArgumentsList(setter, delegateSetter, isGetter = false, receiverProvider)
            )
        })
    }

    fun IrBuilder.createBoundReceiverExpr(accessor: IrSimpleFunction, backingField: IrField?, remappedReceiver: IrExpression?) =
        backingField?.run { irGetField(accessor.dispatchReceiverParameter?.let(::irGet), this) } ?: remappedReceiver

    fun IrBlockBuilder.createAccessorArgumentsList(
        accessor: IrSimpleFunction,
        delegateAccessor: IrSimpleFunction,
        isGetter: Boolean,
        receiverProvider: IrBuilder.() -> IrExpression?,
    ): List<IrValueDeclaration> {
        val boundReceiverOrNull = receiverProvider()
        val setterParam = if (isGetter) null else accessor.parameters.lastOrNull() ?: error("setter must have at least one parameter")
        return buildList {
            if (boundReceiverOrNull != null) add(createTmpVariable(boundReceiverOrNull.deepCopyWithSymbols(accessor)))
            if (size + (if (isGetter) 0 else 1) < delegateAccessor.parameters.size) {
                val unboundReceiver = accessor.getReceiverParameterOrNull()
                if (unboundReceiver != null) add(unboundReceiver)
            }
            if (setterParam != null) add(setterParam)
        }.also {
            require(it.size == delegateAccessor.parameters.size) {
                "delegate accessor needs ${delegateAccessor.parameters.size} arguments, but only ${it.size} found"
            }
        }
    }

    private fun IrProperty.transform(): List<IrDeclaration>? {
        val delegate = getRichPropertyReferenceForOptimizableDelegatedProperty() ?: return null
        val oldField = backingField ?: return null
        val boundValueOrNull = delegate.singleBoundValueOrNull?.transform(this@PropertyReferenceDelegationTransformer, null)
        backingField = boundValueOrNull?.takeIf { !it.canInline(parents.toSet()) }?.let {
            context.irFactory.buildField {
                updateFrom(oldField)
                name = Name.identifier("${this@transform.name}\$receiver")
                type = boundValueOrNull.type
            }.apply {
                parent = oldField.parent
                initializer = context.irFactory.createExpressionBody(it)
                correspondingPropertySymbol = oldField.correspondingPropertySymbol
            }
        }
        val originalThis = parentAsClass.thisReceiver

        fun remapReceiverIfNeeded(accessor: IrSimpleFunction) = if (backingField == null) {
            boundValueOrNull?.remapReceiver(originalThis, accessor.getReceiverParameterOrNull())
        } else {
            null
        }

        getter?.apply {
            body = with(context.createIrBuilder(symbol, startOffset, endOffset)) {
                createGetterBody(
                    getter = this@apply,
                    delegateReference = delegate,
                    receiverProvider = {
                        createBoundReceiverExpr(this@apply, backingField, remapReceiverIfNeeded(this@apply))
                    }
                )
            }
        }
        setter?.apply {
            body = with(context.createIrBuilder(symbol, startOffset, endOffset)) {
                createSetterBody(
                    setter = this@apply,
                    delegateReference = delegate,
                    receiverProvider = {
                        createBoundReceiverExpr(this@apply, backingField, remapReceiverIfNeeded(this@apply))
                    }
                )
            }
        }

        // The `$delegate` method is generated as instance method here, see MakePropertyDelegateMethodsStaticLowering.
        val delegateMethod = context.createSyntheticMethodForPropertyDelegate(this).apply {
            body = context.createJvmIrBuilder(symbol).run {
                val boundReceiver = createBoundReceiverExpr(this@apply, backingField, remapReceiverIfNeeded(this@apply))
                irExprBody(
                    delegate.deepCopyWithSymbols(parent).apply {
                        origin = PropertyReferenceLowering.REFLECTED_PROPERTY_REFERENCE
                        if (boundReceiver != null) {
                            boundValues.clear()
                            boundValues.add(boundReceiver)
                        }
                    })
            }
        }
        // When the receiver is inlined, it can have side effects in form of class initialization, so it should be evaluated here.
        val receiverBlock = boundValueOrNull.takeIf { backingField == null }?.let {
            val symbol = IrAnonymousInitializerSymbolImpl(parentAsClass.symbol)
            context.irFactory.createAnonymousInitializer(
                it.startOffset,
                it.endOffset,
                IrDeclarationOrigin.DEFINED,
                symbol,
                parentAsClass.isFacadeClass
            ).apply {
                body = context.irFactory.createBlockBody(startOffset, endOffset, listOf(it.remapReceiver(null, null)))
            }
        }
        return listOfNotNull(this, delegateMethod, receiverBlock)
    }

    private fun IrFunction.getReceiverParameterOrNull(): IrValueParameter? {
        return parameters.find { it.kind == IrParameterKind.ExtensionReceiver } ?: dispatchReceiverParameter
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
        val delegate = declaration.delegate
        val delegateInitializer = delegate?.initializer
        if (delegateInitializer !is IrRichPropertyReference ||
            !declaration.getter.returnsResultOfStdlibCall ||
            declaration.setter?.returnsResultOfStdlibCall == false
        ) return super.visitLocalDelegatedProperty(declaration)
        val receiver = delegateInitializer.singleBoundValueOrNull?.let { receiver ->
            with(delegate) {
                buildVariable(parent, startOffset, endOffset, origin, name, receiver.type)
            }.apply {
                initializer = receiver.transform(this@PropertyReferenceDelegationTransformer, null)
            }
        }        // TODO: just like in `PropertyReferenceLowering`, probably better to inline the getter/setter rather than
        //       generate them as local functions.
        val getter = declaration.getter.apply {
            with(context.createIrBuilder(symbol, startOffset, endOffset)) {
                body = createGetterBody(
                    getter = this@apply,
                    delegateReference = delegateInitializer,
                    receiverProvider = { receiver?.let { irGet(it) } }
                )
            }
        }
        val setter = declaration.setter?.apply {
            with(context.createIrBuilder(symbol, startOffset, endOffset)) {
                body = createSetterBody(
                    setter = this@apply,
                    delegateReference = delegateInitializer,
                    receiverProvider = { receiver?.let { irGet(it) } }
                )
            }
        }
        val statements = listOfNotNull(receiver, getter, setter)
        return statements.singleOrNull()
            ?: IrCompositeImpl(declaration.startOffset, declaration.endOffset, context.irBuiltIns.unitType, null, statements)
    }
}
