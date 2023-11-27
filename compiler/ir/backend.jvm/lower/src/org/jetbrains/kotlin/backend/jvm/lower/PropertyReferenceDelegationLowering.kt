/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.util.parents
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.fileParentOrNull
import org.jetbrains.kotlin.backend.jvm.lower.JvmPropertiesLowering.Companion.createSyntheticMethodForPropertyDelegate
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrPropertyReferenceImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name

internal val propertyReferenceDelegationPhase = makeIrFilePhase(
    ::PropertyReferenceDelegationLowering,
    name = "PropertyReferenceDelegation",
    description = "Optimize `val x by ::y`: there is no need to construct a KProperty instance"
)

private class PropertyReferenceDelegationLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        if (!context.config.generateOptimizedCallableReferenceSuperClasses) return
        irFile.transform(PropertyReferenceDelegationTransformer(context), null)
    }
}

private class PropertyReferenceDelegationTransformer(val context: JvmBackendContext) : IrElementTransformerVoid() {
    private fun IrSimpleFunction.accessorBody(delegate: IrPropertyReference, receiverFieldOrExpression: IrStatement?): IrBody =
        context.createIrBuilder(symbol, startOffset, endOffset).run {
            val value = valueParameters.singleOrNull()?.let(::irGet)
            val isGetter = value == null
            if (isGetter) {
                delegate.constInitializer?.let { return@run irExprBody(it) }
            }

            var boundReceiver = when (receiverFieldOrExpression) {
                null -> null
                is IrField -> irGetField(dispatchReceiverParameter?.let(::irGet), receiverFieldOrExpression)
                is IrValueDeclaration -> irGet(receiverFieldOrExpression)
                is IrExpression -> receiverFieldOrExpression
                else -> throw AssertionError("not a field/variable/expression: ${receiverFieldOrExpression.render()}")
            }
            val unboundReceiver = extensionReceiverParameter ?: dispatchReceiverParameter
            val field = delegate.field?.owner
            val access = if (field == null) {
                val accessor = if (isGetter) delegate.getter!! else delegate.setter!!
                irCall(accessor).apply {
                    // This has the same assumptions about receivers as `PropertyReferenceLowering.propertyReferenceKindFor`:
                    // only one receiver can be bound, and if the property has both, the extension receiver cannot be bound.
                    // The frontend must also ensure the receiver of the delegated property (extension if present, dispatch
                    // otherwise) is a subtype of the unbound receiver (if there is one; and there can *only* be one).
                    if (accessor.owner.dispatchReceiverParameter != null) {
                        dispatchReceiver = boundReceiver.also { boundReceiver = null } ?: irGet(unboundReceiver!!)
                    }
                    if (accessor.owner.extensionReceiverParameter != null) {
                        extensionReceiver = boundReceiver.also { boundReceiver = null } ?: irGet(unboundReceiver!!)
                    }
                    if (value != null) {
                        putValueArgument(0, value)
                    }
                }
            } else {
                val receiver = if (field.isStatic) null else boundReceiver ?: irGet(unboundReceiver!!)
                if (isGetter) irGetField(receiver, field) else irSetField(receiver, field, value!!)
            }
            irExprBody(access)
        }

    // Some receivers don't need to be stored in fields and can be reevaluated every time an accessor is called:
    private fun IrExpression.canInline(visibleScopes: Set<IrDeclarationParent>): Boolean = when (this) {
        is IrGetValue -> {
            // Reads of immutable variables are stable, but value parameters of the constructor are not in scope:
            val value = symbol.owner
            !(value is IrVariable && value.isVar) && value.parent in visibleScopes
        }
        is IrGetField -> {
            // Reads of final fields of stable values are stable, but fields in other files can become non-final:
            val field = symbol.owner
            field.isFinal && field.fileParentOrNull.let { it != null && it in visibleScopes }
                    && receiver?.canInline(visibleScopes) != false
        }
        is IrCall -> {
            // Same applies to reads of properties with default getters, but non-final properties may be overridden by `var`s:
            val callee = symbol.owner
            callee.isFinalDefaultValGetter && callee.fileParentOrNull.let { it != null && it in visibleScopes }
                    && dispatchReceiver?.canInline(visibleScopes) != false
                    && extensionReceiver?.canInline(visibleScopes) != false
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

    private fun IrProperty.transform(): List<IrDeclaration>? {
        val delegate = getPropertyReferenceForOptimizableDelegatedProperty() ?: return null
        val oldField = backingField ?: return null

        val receiver = (delegate.dispatchReceiver ?: delegate.extensionReceiver)
            ?.transform(this@PropertyReferenceDelegationTransformer, null)
        backingField = receiver?.takeIf { !it.canInline(parents.toSet()) }?.let {
            context.irFactory.buildField {
                updateFrom(oldField)
                name = Name.identifier("${this@transform.name}\$receiver")
                type = receiver.type
            }.apply {
                parent = oldField.parent
                initializer = context.irFactory.createExpressionBody(it)
                correspondingPropertySymbol = oldField.correspondingPropertySymbol
            }
        }
        val originalThis = parentAsClass.thisReceiver
        getter?.apply { body = accessorBody(delegate, backingField ?: receiver?.remapReceiver(originalThis, dispatchReceiverParameter)) }
        setter?.apply { body = accessorBody(delegate, backingField ?: receiver?.remapReceiver(originalThis, dispatchReceiverParameter)) }

        // The `$delegate` method is generated as instance method here, see MakePropertyDelegateMethodsStaticLowering.
        val delegateMethod = context.createSyntheticMethodForPropertyDelegate(this).apply {
            body = context.createJvmIrBuilder(symbol).run {
                val boundReceiver = backingField?.let { irGetField(dispatchReceiverParameter?.let(::irGet), it) }
                    ?: receiver?.remapReceiver(originalThis, dispatchReceiverParameter)
                irExprBody(with(delegate) {
                    val origin = PropertyReferenceLowering.REFLECTED_PROPERTY_REFERENCE
                    IrPropertyReferenceImpl(startOffset, endOffset, type, symbol, typeArgumentsCount, field, getter, setter, origin)
                }.apply {
                    when {
                        delegate.dispatchReceiver != null -> dispatchReceiver = boundReceiver
                        delegate.extensionReceiver != null -> extensionReceiver = boundReceiver
                    }
                })
            }
        }
        // When the receiver is inlined, it can have side effects in form of class initialization, so it should be evaluated here.
        val receiverBlock = receiver.takeIf { backingField == null }?.let {
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

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
        val delegate = declaration.delegate.initializer
        if (delegate !is IrPropertyReference ||
            !declaration.getter.returnsResultOfStdlibCall ||
            declaration.setter?.returnsResultOfStdlibCall == false
        ) return super.visitLocalDelegatedProperty(declaration)

        // Variables are cheap, so optimizing them out is not really necessary.
        val receiver = (delegate.dispatchReceiver ?: delegate.extensionReceiver)?.let { receiver ->
            with(declaration.delegate) { buildVariable(parent, startOffset, endOffset, origin, name, receiver.type) }.apply {
                initializer = receiver.transform(this@PropertyReferenceDelegationTransformer, null)
            }
        }
        // TODO: just like in `PropertyReferenceLowering`, probably better to inline the getter/setter rather than
        //       generate them as local functions.
        val getter = declaration.getter.apply { body = accessorBody(delegate, receiver) }
        val setter = declaration.setter?.apply { body = accessorBody(delegate, receiver) }
        val statements = listOfNotNull(receiver, getter, setter)
        return statements.singleOrNull()
            ?: IrCompositeImpl(declaration.startOffset, declaration.endOffset, context.irBuiltIns.unitType, null, statements)
    }
}
