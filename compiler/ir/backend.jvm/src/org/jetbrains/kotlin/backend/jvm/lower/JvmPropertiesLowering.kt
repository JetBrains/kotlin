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
import org.jetbrains.kotlin.backend.jvm.ir.eraseTypeParameters
import org.jetbrains.kotlin.backend.jvm.ir.needsAccessor
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.hasMangledReturnType
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.requiresMangling
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull

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

        if (shouldSubstituteAccessorWithField(property, simpleFunction) ||
            isDefaultAccessorForCompanionPropertyBackingFieldOnCurrentClass(property, simpleFunction)
        ) {
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

    private fun isDefaultAccessorForCompanionPropertyBackingFieldOnCurrentClass(
        property: IrProperty,
        function: IrSimpleFunction
    ): Boolean {
        if (function.origin != IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR) return false
        if (property.isLateinit) return false
        // If this code could end up inlined in another class (either an inline function or an
        // inlined lambda in an inline function) use the companion object accessor. Otherwise,
        // we could break binary compatibility if we only recompile the class with the companion
        // object and change to non-default field accessors. The inlined code would still attempt
        // to get the backing field which would no longer exist.
        val inInlineFunctionScope = allScopes.any { scope -> (scope.irElement as? IrFunction)?.isInline ?: false }
        if (inInlineFunctionScope) return false
        val backingField = property.resolveFakeOverride()!!.backingField
        return backingField?.parent == currentClass?.irElement &&
                backingField?.origin == JvmLoweredDeclarationOrigin.COMPANION_PROPERTY_BACKING_FIELD
    }

    private fun IrBuilderWithScope.substituteSetter(irProperty: IrProperty, expression: IrCall): IrExpression {
        val backingField = irProperty.resolveFakeOverride()!!.backingField!!
        return patchReceiver(
            irSetField(
                patchFieldAccessReceiver(expression, irProperty),
                backingField,
                expression.getValueArgument(0)!!
            )
        )
    }

    private fun IrBuilderWithScope.substituteGetter(irProperty: IrProperty, expression: IrCall): IrExpression {
        val backingField = irProperty.resolveFakeOverride()!!.backingField!!
        val value = irGetField(patchFieldAccessReceiver(expression, irProperty), backingField)
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

    private fun IrBuilderWithScope.patchFieldAccessReceiver(expression: IrCall, irProperty: IrProperty): IrExpression? {
        val receiver = expression.dispatchReceiver
        if (receiver != null) {
            val propertyParent = irProperty.parent
            if (propertyParent is IrClass && propertyParent.symbol != receiver.type.classifierOrNull) {
                return irImplicitCast(receiver, propertyParent.defaultType)
            }
        }
        return receiver
    }

    private fun IrBuilderWithScope.patchReceiver(expression: IrFieldAccessExpression): IrExpression =
        if (expression.symbol.owner.isStatic && expression.receiver != null) {
            irBlock {
                +expression.receiver!!.coerceToUnit(context.irBuiltIns, backendContext.typeSystem)
                expression.receiver = null
                +expression
            }
        } else {
            expression
        }

    private fun lowerProperty(declaration: IrProperty, kind: ClassKind): List<IrDeclaration> =
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

    private fun shouldSubstituteAccessorWithField(property: IrProperty, accessor: IrSimpleFunction?): Boolean =
        accessor != null && !property.needsAccessor(accessor)

    private fun createSyntheticMethodForAnnotations(declaration: IrProperty): IrSimpleFunction =
        backendContext.createSyntheticMethodForProperty(
            declaration,
            JvmAbi.ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX,
            JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS,
            // TODO: technically JVM permits having fields with same name but different type, so we could potentially
            //   generate two properties like that; should this be the getter's return type instead?
            isStatic = true, returnType = backendContext.irBuiltIns.unitType
        ).apply {
            body = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
            annotations = declaration.annotations
        }

    companion object {
        private fun JvmBackendContext.createSyntheticMethodForProperty(
            declaration: IrProperty,
            suffix: String,
            origin: IrDeclarationOrigin,
            isStatic: Boolean,
            returnType: IrType
        ) = irFactory.buildFun {
            name = Name.identifier(computeSyntheticMethodName(declaration, suffix))
            modality = Modality.OPEN
            visibility = declaration.visibility
            this.origin = origin
            this.returnType = returnType
        }.apply {
            var index = 0
            valueParameters = listOfNotNull(
                declaration.getter?.dispatchReceiverParameter?.takeIf { !isStatic }?.let {
                    // Synthetic methods don't get generic type signatures anyway, so not exactly useful to preserve type parameters.
                    it.copyTo(this, type = it.type.eraseTypeParameters(), index = index++)
                },
                declaration.getter?.extensionReceiverParameter?.let {
                    it.copyTo(this, type = it.type.eraseTypeParameters(), index = index)
                }
            )
            parent = declaration.parent
            metadata = declaration.metadata
        }

        fun JvmBackendContext.createSyntheticMethodForPropertyDelegate(declaration: IrProperty) =
            createSyntheticMethodForProperty(
                declaration,
                JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX,
                IrDeclarationOrigin.PROPERTY_DELEGATE,
                isStatic = false, returnType = irBuiltIns.anyNType
            )

        private fun JvmBackendContext.computeSyntheticMethodName(property: IrProperty, suffix: String): String {
            val baseName =
                if (state.languageVersionSettings.supportsFeature(LanguageFeature.UseGetterNameForPropertyAnnotationsMethodOnJvm)) {
                    val getter = property.getter
                    if (getter != null) {
                        val needsMangling =
                            getter.extensionReceiverParameter?.type?.requiresMangling == true ||
                                    (state.functionsWithInlineClassReturnTypesMangled && getter.hasMangledReturnType)
                        val mangled = if (needsMangling) inlineClassReplacements.getReplacementFunction(getter) else null
                        methodSignatureMapper.mapFunctionName(mangled ?: getter)
                    } else JvmAbi.getterName(property.name.asString())
                } else {
                    property.name.asString()
                }
            return baseName + suffix
        }
    }
}
