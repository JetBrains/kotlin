/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.render
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
    private fun IrSimpleFunction.accessorBody(delegate: IrPropertyReference, receiverField: IrDeclaration?) =
        context.createIrBuilder(symbol, startOffset, endOffset).run {
            val value = valueParameters.singleOrNull()?.let(::irGet)
            var boundReceiver = when (receiverField) {
                null -> null
                is IrField -> irGetField(dispatchReceiverParameter?.let(::irGet), receiverField)
                is IrValueDeclaration -> irGet(receiverField)
                else -> throw AssertionError("not a field/variable: ${receiverField.render()}")
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

    override fun visitProperty(declaration: IrProperty): IrStatement {
        if (!declaration.isDelegated || declaration.isFakeOverride) return super.visitProperty(declaration)

        val oldField = declaration.backingField
        val delegate = oldField?.initializer?.expression
        if (delegate !is IrPropertyReference ||
            declaration.getter?.returnsResultOfStdlibCall == false ||
            declaration.setter?.returnsResultOfStdlibCall == false
        ) return super.visitProperty(declaration)

        declaration.backingField = (delegate.dispatchReceiver ?: delegate.extensionReceiver)?.let { receiver ->
            context.irFactory.buildField {
                updateFrom(oldField)
                name = Name.identifier("${declaration.name}\$receiver")
                type = receiver.type
            }.apply {
                parent = oldField.parent
                initializer = context.irFactory.createExpressionBody(receiver.transform(this@PropertyReferenceDelegationTransformer, null))
            }
        }
        declaration.getter?.apply { body = accessorBody(delegate, declaration.backingField) }
        declaration.setter?.apply { body = accessorBody(delegate, declaration.backingField) }
        return declaration
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
        val delegate = declaration.delegate.initializer
        if (delegate !is IrPropertyReference ||
            !declaration.getter.returnsResultOfStdlibCall ||
            declaration.setter?.returnsResultOfStdlibCall == false
        ) return super.visitLocalDelegatedProperty(declaration)

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
