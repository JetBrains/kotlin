/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.isJvmInterface
import org.jetbrains.kotlin.backend.jvm.ir.replaceThisByStaticReference
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrAnonymousInitializerImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedFieldDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME

internal val moveOrCopyCompanionObjectFieldsPhase = makeIrFilePhase(
    ::MoveOrCopyCompanionObjectFieldsLowering,
    name = "MoveOrCopyCompanionObjectFields",
    description = "Move and/or copy companion object fields to static fields of companion's owner"
)

private class MoveOrCopyCompanionObjectFieldsLowering(val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val fieldReplacementMap = mutableMapOf<IrFieldSymbol, IrFieldSymbol>()
        if (irClass.isObject && !irClass.isCompanion && irClass.visibility != Visibilities.LOCAL) {
            handleObject(irClass, fieldReplacementMap)
        } else {
            handleClass(irClass, fieldReplacementMap)
            if (irClass.isJvmInterface)
                copyConsts(irClass)
        }
        irClass.replaceFieldReferences(fieldReplacementMap)
    }

    private fun handleObject(irObject: IrClass, fieldReplacementMap: MutableMap<IrFieldSymbol, IrFieldSymbol>) {
        irObject.declarations.replaceAll {
            when (it) {
                is IrProperty -> {
                    // The field is not actually moved, just replaced by a static field
                    movePropertyFieldToStaticParent(it, irObject, irObject, fieldReplacementMap)
                    it
                }
                is IrAnonymousInitializer -> moveAnonymousInitializerToStaticParent(it, irObject, irObject)
                else -> it
            }
        }
    }

    private fun handleClass(irClass: IrClass, fieldReplacementMap: MutableMap<IrFieldSymbol, IrFieldSymbol>) {
        val companion = irClass.declarations.find {
            it is IrClass && it.isCompanion
        } as IrClass? ?: return

        // We don't move fields to interfaces unless all fields are annotated with @JvmField.
        // It is an error to annotate only some of the fields of an interface companion with @JvmField.
        val newParent = if (irClass.isJvmInterface && !companion.allFieldsAreJvmField()) companion else irClass

        val newDeclarations = companion.declarations.map {
            when (it) {
                is IrProperty ->
                    movePropertyFieldToStaticParent(it, companion, newParent, fieldReplacementMap)
                is IrAnonymousInitializer ->
                    moveAnonymousInitializerToStaticParent(it, companion, newParent)
                else ->
                    null
            }
        }

        if (newParent === companion) {
            // Keep fields as children of `IrProperty`, but replace anonymous initializers with static ones,
            // preserving the relative ordering of anonymous initializers and property initializers.
            for ((i, declaration) in newDeclarations.withIndex())
                if (declaration is IrAnonymousInitializer)
                    companion.declarations[i] = declaration
        } else {
            // Move all touched declarations to the parent.
            companion.declarations.removeAll { it is IrAnonymousInitializer }
            newDeclarations.filterNotNullTo(newParent.declarations)
        }
    }

    private fun copyConsts(irClass: IrClass) {
        val companion = irClass.declarations.find {
            it is IrClass && it.isCompanion
        } as IrClass? ?: return
        companion.declarations.filter { it is IrProperty && it.isConst && it.hasPublicVisibility }
            .mapNotNullTo(irClass.declarations) { copyPropertyFieldToStaticParent(it as IrProperty, companion, irClass) }
    }

    private val IrProperty.hasPublicVisibility: Boolean
        get() = !Visibilities.isPrivate(visibility) && visibility != Visibilities.PROTECTED

    private fun IrClass.allFieldsAreJvmField() =
        declarations.filterIsInstance<IrProperty>()
            .mapNotNull { it.backingField }.all { it.hasAnnotation(JVM_FIELD_ANNOTATION_FQ_NAME) }

    // If fieldReplacementMap is null / unspecified, keep the old field and don't update the references.
    private fun moveOrCopyPropertyFieldToStaticParent(
        irProperty: IrProperty,
        propertyParent: IrClass,
        fieldParent: IrClass,
        fieldReplacementMap: MutableMap<IrFieldSymbol, IrFieldSymbol>? = null
    ): IrField? {
        if (irProperty.origin == IrDeclarationOrigin.FAKE_OVERRIDE) return null
        val oldField = irProperty.backingField ?: return null
        val newField = createStaticBackingField(oldField, propertyParent, fieldParent)

        fieldReplacementMap?.run {
            irProperty.backingField = newField
            newField.correspondingPropertySymbol = irProperty.symbol
            put(oldField.symbol, newField.symbol)
        }

        return newField
    }

    private fun movePropertyFieldToStaticParent(
        irProperty: IrProperty,
        propertyParent: IrClass,
        fieldParent: IrClass,
        fieldReplacementMap: MutableMap<IrFieldSymbol, IrFieldSymbol>? = null
    ): IrField? = moveOrCopyPropertyFieldToStaticParent(irProperty, propertyParent, fieldParent, fieldReplacementMap)

    private fun copyPropertyFieldToStaticParent(
        irProperty: IrProperty,
        propertyParent: IrClass,
        fieldParent: IrClass
    ): IrField? = moveOrCopyPropertyFieldToStaticParent(irProperty, propertyParent, fieldParent)

    private fun moveAnonymousInitializerToStaticParent(
        oldInitializer: IrAnonymousInitializer,
        oldParent: IrClass,
        newParent: IrClass
    ): IrAnonymousInitializer =
        with(oldInitializer) {
            IrAnonymousInitializerImpl(
                startOffset, endOffset, origin, IrAnonymousInitializerSymbolImpl(newParent.symbol),
                isStatic = true
            ).apply {
                parent = newParent
                body = oldInitializer.body
                    .replaceThisByStaticReference(context, oldParent, oldParent.thisReceiver!!)
                    .patchDeclarationParents(newParent) as IrBlockBody
            }
        }

    private fun createStaticBackingField(oldField: IrField, propertyParent: IrClass, fieldParent: IrClass): IrField {
        val descriptor = WrappedFieldDescriptor(oldField.descriptor.annotations, oldField.descriptor.source)
        val field = IrFieldImpl(
            oldField.startOffset, oldField.endOffset,
            IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
            IrFieldSymbolImpl(descriptor),
            oldField.name, oldField.type, oldField.visibility,
            isFinal = oldField.isFinal,
            isExternal = oldField.isExternal,
            isStatic = true,
            isFakeOverride = false
        ).apply {
            descriptor.bind(this)
            parent = fieldParent
            annotations.addAll(oldField.annotations)
            metadata = oldField.metadata
        }
        val oldInitializer = oldField.initializer
        if (oldInitializer != null) {
            field.initializer = oldInitializer
                .replaceThisByStaticReference(context, propertyParent, propertyParent.thisReceiver!!)
                .deepCopyWithSymbols(field) as IrExpressionBody
        }

        return field
    }
}

private fun IrElement.replaceFieldReferences(replacementMap: Map<IrFieldSymbol, IrFieldSymbol>) {
    transformChildrenVoid(FieldReplacer(replacementMap))
}

private class FieldReplacer(val replacementMap: Map<IrFieldSymbol, IrFieldSymbol>) : IrElementTransformerVoid() {
    override fun visitGetField(expression: IrGetField): IrExpression =
        replacementMap[expression.symbol]?.let { newSymbol ->
            IrGetFieldImpl(
                expression.startOffset, expression.endOffset,
                newSymbol,
                expression.type,
                /* receiver = */ null,
                expression.origin,
                expression.superQualifierSymbol
            )
        } ?: super.visitGetField(expression)

    override fun visitSetField(expression: IrSetField): IrExpression =
        replacementMap[expression.symbol]?.let { _ ->
            IrSetFieldImpl(
                expression.startOffset, expression.endOffset,
                replacementMap.getValue(expression.symbol),
                /* receiver = */ null,
                visitExpression(expression.value),
                expression.type,
                expression.origin,
                expression.superQualifierSymbol
            )
        } ?: super.visitSetField(expression)
}
