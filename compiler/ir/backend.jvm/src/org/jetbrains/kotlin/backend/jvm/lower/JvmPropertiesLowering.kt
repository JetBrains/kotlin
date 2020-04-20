/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.coerceToUnit
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

class JvmPropertiesLowering(private val backendContext: JvmBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
    }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        declaration.transformChildrenVoid(this)
        declaration.transformDeclarationsFlat { if (it is IrProperty) lowerProperty(it, declaration.kind) else null }
        return declaration
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val simpleFunction = (expression.symbol.owner as? IrSimpleFunction) ?: return super.visitCall(expression)
        val property = simpleFunction.correspondingPropertySymbol?.owner ?: return super.visitCall(expression)
        expression.transformChildrenVoid()

        if (shouldSubstituteAccessorWithField(property, simpleFunction)) {
            backendContext.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset).apply {
                return when (simpleFunction) {
                    property.getter -> substituteGetter(property, expression)
                    property.setter -> substituteSetter(property, expression)
                    else -> error("Orphaned property getter/setter: ${simpleFunction.render()}")
                }
            }
        }

        return expression
    }

    private fun IrBuilderWithScope.substituteSetter(irProperty: IrProperty, expression: IrCall): IrExpression =
        patchReceiver(irSetField(expression.dispatchReceiver, irProperty.backingField!!, expression.getValueArgument(0)!!))

    private fun IrBuilderWithScope.substituteGetter(irProperty: IrProperty, expression: IrCall): IrExpression {
        val backingField = irProperty.backingField!!
        val value = irGetField(expression.dispatchReceiver, backingField)
        return if (irProperty.isLateinit) {
            irBlock {
                val tmpVal = irTemporary(value)
                +irIfNull(
                    expression.type.makeNotNull(),
                    irGet(tmpVal),
                    backendContext.throwUninitializedPropertyAccessException(this, backingField.name.asString()),
                    irGet(tmpVal)
                )
            }
        } else {
            value
        }
    }

    private fun IrBuilderWithScope.patchReceiver(expression: IrFieldAccessExpression): IrExpression =
        if (expression.symbol.owner.isStatic && expression.receiver != null) {
            irBlock {
                +expression.receiver!!.coerceToUnit(context.irBuiltIns)
                expression.receiver = null
                +expression
            }
        } else {
            expression
        }

    private fun lowerProperty(declaration: IrProperty, kind: ClassKind): List<IrDeclaration>? =
        ArrayList<IrDeclaration>(4).apply {
            val field = declaration.backingField

            // JvmFields in a companion object refer to companion's owners and should not be generated within companion.
            if ((kind != ClassKind.ANNOTATION_CLASS || field?.isStatic == true) && field?.parent == declaration.parent) {
                addIfNotNull(field)
            }

            if (!declaration.isConst) {
                declaration.getter?.takeIf { !shouldSubstituteAccessorWithField(declaration, it) }?.let { add(it) }
                declaration.setter?.takeIf { !shouldSubstituteAccessorWithField(declaration, it) }?.let { add(it) }
            }

            if (!declaration.isFakeOverride && declaration.annotations.isNotEmpty()) {
                add(createSyntheticMethodForAnnotations(declaration))
            }
        }

    private fun shouldSubstituteAccessorWithField(property: IrProperty, accessor: IrSimpleFunction?): Boolean {
        if (accessor == null) return false

        if ((property.parent as? IrClass)?.kind == ClassKind.ANNOTATION_CLASS) return false

        if (property.backingField?.hasAnnotation(JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME) == true) return true

        return accessor.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR && Visibilities.isPrivate(accessor.visibility)
    }

    private fun createSyntheticMethodForAnnotations(declaration: IrProperty): IrFunctionImpl =
        buildFun {
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS
            name = Name.identifier(computeSyntheticMethodName(declaration))
            visibility = declaration.visibility
            modality = Modality.OPEN
            returnType = backendContext.irBuiltIns.unitType
        }.apply {
            declaration.getter?.extensionReceiverParameter?.let { extensionReceiver ->
                // Use raw type of extension receiver to avoid generic signature, which would be useless for this method.
                extensionReceiverParameter = extensionReceiver.copyTo(this, type = extensionReceiver.type.classifierOrFail.typeWith())
            }

            body = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
            parent = declaration.parent

            annotations = declaration.annotations
            metadata = declaration.metadata
        }

    private fun computeSyntheticMethodName(property: IrProperty): String {
        val baseName =
            if (backendContext.state.languageVersionSettings.supportsFeature(LanguageFeature.UseGetterNameForPropertyAnnotationsMethodOnJvm)) {
                property.getter?.let { getter ->
                    backendContext.methodSignatureMapper.mapFunctionName(getter)
                } ?: JvmAbi.getterName(property.name.asString())
            } else {
                property.name.asString()
            }
        return JvmAbi.getSyntheticMethodNameForAnnotatedProperty(baseName)
    }
}
