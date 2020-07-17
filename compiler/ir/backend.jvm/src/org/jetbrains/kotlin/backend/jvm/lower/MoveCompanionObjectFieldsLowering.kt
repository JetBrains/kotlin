/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.replaceThisByStaticReference
import org.jetbrains.kotlin.backend.jvm.propertiesPhase
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrAnonymousInitializerImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

internal val moveOrCopyCompanionObjectFieldsPhase = makeIrFilePhase(
    ::MoveOrCopyCompanionObjectFieldsLowering,
    name = "MoveOrCopyCompanionObjectFields",
    description = "Move and/or copy companion object fields to static fields of companion's owner"
)

internal val remapObjectFieldAccesses = makeIrFilePhase(
    ::RemapObjectFieldAccesses,
    name = "RemapObjectFieldAccesses",
    description = "Make IrGetField/IrSetField to objects' fields point to the static versions",
    prerequisite = setOf(propertiesPhase)
)

private class MoveOrCopyCompanionObjectFieldsLowering(val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        if (irClass.isObject && !irClass.isCompanion) {
            irClass.handle()
        } else {
            (irClass.declarations.singleOrNull { it is IrClass && it.isCompanion } as IrClass?)?.handle()
        }
    }

    private fun IrClass.handle() {
        val newDeclarations = declarations.map {
            when (it) {
                is IrProperty -> context.declarationFactory.getStaticBackingField(it)?.also { newField ->
                    it.backingField = newField
                    newField.correspondingPropertySymbol = it.symbol
                }
                else -> null
            }
        }

        val companionParent = if (isCompanion) parentAsClass else null
        // In case a companion contains no fields, move the anonymous initializers to the parent
        // anyway, as otherwise there will probably be no references to the companion class at all
        // and therefore its class initializer will never be invoked.
        val newParent = newDeclarations.firstOrNull { it != null }?.parentAsClass ?: companionParent ?: this
        if (newParent === this) {
            declarations.replaceAll {
                // Anonymous initializers must be made static to correctly initialize the new static
                // fields when the class is loaded.
                if (it is IrAnonymousInitializer) makeAnonymousInitializerStatic(it, newParent) else it
            }

            if (companionParent != null) {
                for (declaration in declarations) {
                    if (declaration is IrProperty && declaration.isConst && declaration.hasPublicVisibility) {
                        copyConstProperty(declaration, companionParent)
                    }
                }
            }
        } else {
            // Anonymous initializers must also be moved and their ordering relative to the fields
            // must be preserved, as the fields can have expression initializers themselves.
            for ((i, newField) in newDeclarations.withIndex()) {
                if (newField != null)
                    newParent.declarations += newField
                if (declarations[i] is IrAnonymousInitializer)
                    newParent.declarations += makeAnonymousInitializerStatic(declarations[i] as IrAnonymousInitializer, newParent)
            }
            declarations.removeAll { it is IrAnonymousInitializer }
        }
    }

    private val IrProperty.hasPublicVisibility: Boolean
        get() = !Visibilities.isPrivate(visibility) && visibility != Visibilities.PROTECTED

    private fun makeAnonymousInitializerStatic(oldInitializer: IrAnonymousInitializer, newParent: IrClass) =
        with(oldInitializer) {
            val oldParent = parentAsClass
            val newSymbol = IrAnonymousInitializerSymbolImpl(newParent.symbol)
            IrAnonymousInitializerImpl(startOffset, endOffset, origin, newSymbol, isStatic = true).apply {
                parent = newParent
                body = this@with.body
                    .replaceThisByStaticReference(context.declarationFactory, oldParent, oldParent.thisReceiver!!)
                    .patchDeclarationParents(newParent) as IrBlockBody
            }
        }

    private fun copyConstProperty(oldProperty: IrProperty, newParent: IrClass): IrProperty =
        newParent.addProperty {
            updateFrom(oldProperty)
            name = oldProperty.name
            isConst = true
        }.also { property ->
            val oldField = oldProperty.backingField!!
            property.backingField = buildField {
                updateFrom(oldField)
                name = oldField.name
                isStatic = true
            }.apply {
                parent = newParent
                correspondingPropertySymbol = property.symbol
                annotations += oldField.annotations
                initializer = with(oldField.initializer!!) {
                    IrExpressionBodyImpl(startOffset, endOffset, (expression as IrConst<*>).copy())
                }
            }
        }
}

private class RemapObjectFieldAccesses(val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoid() {
    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    private fun IrField.remap(): IrField? =
        correspondingPropertySymbol?.owner?.let(context.declarationFactory::getStaticBackingField)

    override fun visitGetField(expression: IrGetField): IrExpression =
        expression.symbol.owner.remap()?.let {
            with(expression) {
                IrGetFieldImpl(startOffset, endOffset, it.symbol, type, /* receiver = */ null, origin, superQualifierSymbol)
            }
        } ?: super.visitGetField(expression)

    override fun visitSetField(expression: IrSetField): IrExpression =
        expression.symbol.owner.remap()?.let {
            with(expression) {
                val newValue = value.transform(this@RemapObjectFieldAccesses, null)
                IrSetFieldImpl(startOffset, endOffset, it.symbol, /* receiver = */ null, newValue, type, origin, superQualifierSymbol)
            }
        } ?: super.visitSetField(expression)
}
