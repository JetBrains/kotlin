/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.fileParent
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.lower.JvmPropertiesLowering.Companion.createSyntheticMethodForPropertyDelegate
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
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
        irFile.transform(PropertyReferenceDelegationTransformer(context), null)
    }
}

private class PropertyReferenceDelegationTransformer(val context: JvmBackendContext) : IrElementTransformerVoid() {
    private fun IrSimpleFunction.accessorBody(delegate: IrPropertyReference, receiverFieldOrExpression: IrStatement?) =
        context.createIrBuilder(symbol, startOffset, endOffset).run {
            val value = valueParameters.singleOrNull()?.let(::irGet)
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
                val accessor = if (value == null) delegate.getter!! else delegate.setter!!
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
                if (value == null) irGetField(receiver, field) else irSetField(receiver, field, value)
            }
            irExprBody(access)
        }

    private val IrSimpleFunction.returnsResultOfStdlibCall: Boolean
        get() = when (val body = body) {
            is IrExpressionBody -> body.expression.isStdlibCall
            is IrBlockBody -> body.statements.singleOrNull()?.let { it.isStdlibCall || (it is IrReturn && it.value.isStdlibCall) } == true
            else -> false
        }

    private val IrStatement.isStdlibCall: Boolean
        get() = this is IrCall && symbol.owner.getPackageFragment()?.fqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME

    // Constants, object accesses, reads of immutable variables, and reads of immutable properties in the same file
    // don't need to be cached in a field; reads of mutable properties have to be as we should ignore further assignments
    // to those, and immutable properties in other files might become mutable without breaking ABI.
    private fun IrExpression.canInline(currentFile: IrFile): Boolean = when (this) {
        is IrGetValue -> !symbol.owner.let { it is IrVariable && it.isVar }
        is IrGetField -> symbol.owner.let { it.isFinal && it.fileParent == currentFile } && receiver?.canInline(currentFile) != false
        is IrCall -> symbol.owner.let { it.isFinalDefaultValGetter && it.fileParent == currentFile } &&
                dispatchReceiver?.canInline(currentFile) != false && extensionReceiver?.canInline(currentFile) != false
        else -> isTrivial()
    }

    private val IrSimpleFunction.isFinalDefaultValGetter: Boolean
        get() = origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR &&
                correspondingPropertySymbol?.let { it.owner.getter == this && it.owner.setter == null } == true &&
                modality == Modality.FINAL

    private fun IrExpression.inline(oldReceiver: IrValueParameter?, newReceiver: IrValueParameter?): IrExpression = when (this) {
        is IrGetField ->
            IrGetFieldImpl(startOffset, endOffset, symbol, type, receiver?.inline(oldReceiver, newReceiver), origin, superQualifierSymbol)
        is IrGetValue ->
            IrGetValueImpl(startOffset, endOffset, type, newReceiver?.symbol.takeIf { symbol == oldReceiver?.symbol } ?: symbol, origin)
        is IrCall ->
            IrCallImpl(startOffset, endOffset, type, symbol, typeArgumentsCount, valueArgumentsCount, origin, superQualifierSymbol).apply {
                dispatchReceiver = this@inline.dispatchReceiver?.inline(oldReceiver, newReceiver)
                extensionReceiver = this@inline.extensionReceiver?.inline(oldReceiver, newReceiver)
            }
        else -> shallowCopy()
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        declaration.transformChildren(this, null)
        declaration.transformDeclarationsFlat {
            (it as? IrProperty)?.transform()
        }
        return declaration
    }

    private fun IrProperty.transform(): List<IrDeclaration>? {
        if (!isDelegated || isFakeOverride) return null

        val oldField = backingField
        val delegate = oldField?.initializer?.expression
        if (delegate !is IrPropertyReference ||
            getter?.returnsResultOfStdlibCall == false ||
            setter?.returnsResultOfStdlibCall == false
        ) return null

        val receiver = (delegate.dispatchReceiver ?: delegate.extensionReceiver)
            ?.transform(this@PropertyReferenceDelegationTransformer, null)
        backingField = receiver?.takeIf { !it.canInline(fileParent) }?.let {
            context.irFactory.buildField {
                updateFrom(oldField)
                name = Name.identifier("${this@transform.name}\$receiver")
                type = receiver.type
            }.apply {
                parent = oldField.parent
                initializer = context.irFactory.createExpressionBody(it)
            }
        }
        val originalThis = parentAsClass.thisReceiver
        getter?.apply { body = accessorBody(delegate, backingField ?: receiver?.inline(originalThis, dispatchReceiverParameter)) }
        setter?.apply { body = accessorBody(delegate, backingField ?: receiver?.inline(originalThis, dispatchReceiverParameter)) }

        val delegateMethod = context.createSyntheticMethodForPropertyDelegate(this).apply {
            body = context.createJvmIrBuilder(symbol).run {
                val propertyOwner = if (getter?.dispatchReceiverParameter != null) valueParameters[0] else null
                val boundReceiver = backingField?.let { irGetField(propertyOwner?.let(::irGet), it) }
                    ?: receiver?.inline(originalThis, propertyOwner)
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
            context.irFactory.createAnonymousInitializer(it.startOffset, it.endOffset, IrDeclarationOrigin.DEFINED, symbol).apply {
                body = context.irFactory.createBlockBody(startOffset, endOffset, listOf(it.inline(null, null)))
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
