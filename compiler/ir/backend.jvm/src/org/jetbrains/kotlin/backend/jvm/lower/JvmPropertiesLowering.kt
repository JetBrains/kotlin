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
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
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
            if (propertyParent is IrClass && propertyParent.symbol != receiver.type.classifierOrNull &&
                expression.superQualifierSymbol == null
            ) {
                return irImplicitCast(receiver, propertyParent.defaultType)
            }
        }
        return receiver
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
        backendContext.irFactory.buildFun {
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS
            name = Name.identifier(computeSyntheticMethodName(declaration))
            visibility = declaration.visibility
            modality = Modality.OPEN
            returnType = backendContext.irBuiltIns.unitType
        }.apply {
            declaration.getter?.extensionReceiverParameter?.let { extensionReceiver ->
                extensionReceiverParameter = extensionReceiver.copyTo(
                    this,
                    type = extensionReceiver.type.erasePropertyAnnotationsExtensionReceiverType()
                )
            }

            body = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
            parent = declaration.parent

            annotations = declaration.annotations
            metadata = declaration.metadata
        }

    private fun IrType.erasePropertyAnnotationsExtensionReceiverType(): IrType {
        // Use raw type of extension receiver to avoid generic signature,
        // which should not be generated for '...$annotations' method.
        if (this !is IrSimpleType) {
            throw AssertionError("Unexpected property receiver type: $this")
        }
        val erasedType = if (isArray()) {
            when (val arg0 = arguments[0]) {
                is IrStarProjection -> {
                    // 'Array<*>' becomes 'Array<*>'
                    this
                }
                is IrTypeProjection -> {
                    // 'Array<VARIANCE TYPE>' becomes 'Array<VARIANCE erase(TYPE)>'
                    classifier.typeWithArguments(
                        listOf(makeTypeProjection(arg0.type.erasePropertyAnnotationsExtensionReceiverType(), arg0.variance))
                    )
                }
                else ->
                    throw AssertionError("Unexpected type argument: $arg0")
            }
        } else {
            classifier.typeWith()
        }
        return erasedType
            .withHasQuestionMark(this.hasQuestionMark)
            .addAnnotations(this.annotations)
    }

    private fun computeSyntheticMethodName(property: IrProperty): String {
        val baseName =
            if (backendContext.state.languageVersionSettings.supportsFeature(LanguageFeature.UseGetterNameForPropertyAnnotationsMethodOnJvm)) {
                val getter = property.getter
                if (getter != null) {
                    val needsMangling =
                        getter.extensionReceiverParameter?.type?.requiresMangling == true ||
                                (backendContext.state.functionsWithInlineClassReturnTypesMangled && getter.hasMangledReturnType)

                    backendContext.methodSignatureMapper.mapFunctionName(
                        if (needsMangling) backendContext.inlineClassReplacements.getReplacementFunction(getter) ?: getter
                        else getter
                    )
                } else JvmAbi.getterName(property.name.asString())
            } else {
                property.name.asString()
            }
        return JvmAbi.getSyntheticMethodNameForAnnotatedProperty(baseName)
    }
}
