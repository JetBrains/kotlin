/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNameOrKotlinName
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrDynamicMemberExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

val EXTERNAL_SUPER_ACCESSORS_ORIGIN by IrDeclarationOriginImpl

/**
 * The lowering saves external properties access with the super qualifier
 * Right now it assumes that the overridden external property is always a field in a JS class.
 * However, there is a possibility to define custom getters/setters on the JS side.
 * We intentionally (until reporting an issue with some popular case) refuse to process such cases.
 * The reason is that in the JS world using of custom getters/setters is a bad practice (and optimizers like Terser or Google Closure Compiler don't work with such a code)
 *
 * The following code:
 * ```kotlin
 *  external open class Foo {
 *      val value: String
 *  }
 *
 *  class GetterOverrider : Foo() { override val value get() = "Foo: ${super.value}" }
 *  class ValueOverrider : Foo() { override val value = "Foo: ${super.value}" }
 * ```
 *
 * Will be lowered to the following JavaScript
 *
 * ```javascript
 * class GetterOverrider extends Foo {
 *      constructor() {
 *          super()
 *          var tmp = this.value;
 *          delete this.value;
 *          this._Foo_super_value = tmp;
 *      }
 *      getValue() {  return `Foo: ${this._Foo_super_value}` }
 *      get value() { return this.getValue(); }
 * }
 * class ValueOverrider extends Foo {
 *      constructor() {
 *          super()
 *          var tmp = this.value;
 *          delete this.value;
 *          this._v = `Foo: ${tmp}`
 *      }
 *      getValue() { return this._v }
 *      get value() { return this.getValue(); }
 * }
 * ```
 *
 * It helps all the following classes to be recompiled without knowledge if they are in a hierarchy with an external class or not,
 * so we can re-compile only a single class instead of the whole hierarchy.
 */
class ExternalPropertyOverridingLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration !is IrClass || declaration.isInterface) return null

        val overriddenExternalPropertyAccessors = declaration.declarations
            .filterIsInstanceAnd<IrSimpleFunction> {
                it.correspondingPropertySymbol != null && !it.isFakeOverride
            }
            .flatMapTo(hashSetOf()) { overriddenAccessor ->
                overriddenAccessor.overriddenSymbols.filter {
                    it.owner.realOverrideTarget.isEffectivelyExternal()
                }
            }

        if (overriddenExternalPropertyAccessors.isEmpty()) return null

        val externalPropertyAccessorsTransformer =
            ExternalPropertySuperAccessTransformer(context, declaration, overriddenExternalPropertyAccessors)

        declaration.transformChildren(externalPropertyAccessorsTransformer, null)

        val positionInConstructorForAccessors = externalPropertyAccessorsTransformer.primaryConstructorBody
            .statements.indexOfFirst { it is IrDelegatingConstructorCall } + 1

        val declaredSuperVariableAndFields = externalPropertyAccessorsTransformer
            .superAccessMap
            .values

        externalPropertyAccessorsTransformer
            .primaryConstructorBody
            .statements
            .addAll(positionInConstructorForAccessors, declaredSuperVariableAndFields.map { it.value })

        declaredSuperVariableAndFields.forEach { (variable, field) ->
            if (field == null) return@forEach
            externalPropertyAccessorsTransformer.primaryConstructorBody.statements.add(
                JsIrBuilder.buildSetField(
                    field.symbol,
                    JsIrBuilder.buildGetValue(externalPropertyAccessorsTransformer.parentClassDispatchReceiver.symbol),
                    JsIrBuilder.buildGetValue(variable.symbol),
                    context.irBuiltIns.unitType
                )
            )
        }

        overriddenExternalPropertyAccessors.forEach {
            if (!it.owner.isGetter) return@forEach

            val accessExpression = with(externalPropertyAccessorsTransformer) { it.createExternalSuperFieldAccess() } ?: return@forEach
            externalPropertyAccessorsTransformer.primaryConstructorBody.statements.add(
                JsIrBuilder.buildCall(context.intrinsics.jsDelete).apply { arguments[0] = accessExpression }
            )
        }

        return null
    }

    private class ExternalPropertySuperAccessTransformer(
        private val context: JsIrBackendContext,
        private val irClass: IrClass,
        private val overriddenExternalPropertiesAccessor: Set<IrSimpleFunctionSymbol>,
    ) : IrTransformer<IrFunction?>() {
        val superAccessMap = mutableMapOf<IrProperty, ExternalPropertySuperAccess>()

        val parentClassPrimaryConstructor = irClass.primaryConstructor
            ?: context.mapping.classToSyntheticPrimaryConstructor[irClass]
            ?: compilationException("Unexpected primary constructor for processing irClass", irClass)

        val primaryConstructorBody = parentClassPrimaryConstructor.body as? IrBlockBody
            ?: compilationException("Unexpected body of primary constructor for processing irClass", parentClassPrimaryConstructor)

        val parentClassDispatchReceiver = irClass.thisReceiver ?: compilationException("Unexpected thisReceiver of class", irClass)

        override fun visitFunction(declaration: IrFunction, data: IrFunction?): IrStatement {
            declaration.transformChildren(this, declaration)
            return declaration
        }

        override fun visitCall(expression: IrCall, data: IrFunction?): IrExpression {
            expression.transformChildren(this, data)
            if (expression.superQualifierSymbol == null || data == null) return expression

            val callee = expression.symbol.owner
            val correspondingProperty = callee.correspondingPropertySymbol?.owner
                ?.takeIf { overriddenExternalPropertiesAccessor.contains(expression.symbol) }
                ?: return expression

            val propertyAccessor = expression.getExternalPropertySuperAccess(correspondingProperty)

            return if (data is IrConstructor && data.isPrimary) {
                when (callee) {
                    correspondingProperty.getter -> JsIrBuilder.buildGetValue(propertyAccessor.value.symbol)
                    correspondingProperty.setter -> JsIrBuilder.buildSetValue(propertyAccessor.value.symbol, expression.setterValue)
                    else -> compilationException("Unexpected accessor of property", callee)
                }
            } else {
                val dispatchReceiver = JsIrBuilder.buildGetValue(parentClassDispatchReceiver.symbol)
                val field = propertyAccessor.field ?: expression.createFieldWithExternalPropertyValue(correspondingProperty)
                    .also { propertyAccessor.field = it }

                when (callee) {
                    correspondingProperty.getter -> JsIrBuilder.buildGetField(field.symbol, dispatchReceiver)
                    correspondingProperty.setter -> JsIrBuilder.buildSetField(
                        field.symbol,
                        dispatchReceiver,
                        expression.setterValue,
                        context.irBuiltIns.unitType
                    )
                    else -> compilationException("Unexpected accessor of property", callee)
                }
            }
        }

        private val IrCall.setterValue: IrExpression
            get() = arguments.last() ?: compilationException("Unexpected setter without argument", this)

        private fun IrCall.getExternalPropertySuperAccess(property: IrProperty) =
            superAccessMap.getOrPut(property) { ExternalPropertySuperAccess(createVariableWithExternalPropertyValue()) }

        private fun IrCall.createVariableWithExternalPropertyValue() =
            JsIrBuilder.buildVar(
                type = type,
                parent = parentClassPrimaryConstructor,
                origin = EXTERNAL_SUPER_ACCESSORS_ORIGIN,
                initializer = symbol.createExternalSuperFieldAccess()
            )

        fun IrSimpleFunctionSymbol.createExternalSuperFieldAccess(): IrExpression? {
            val property = owner.realOverrideTarget.correspondingPropertySymbol?.owner ?: return null
            return IrDynamicMemberExpressionImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                owner.returnType,
                property.getJsNameOrKotlinName().identifier,
                JsIrBuilder.buildGetValue(parentClassDispatchReceiver.symbol)
            )
        }

        private fun IrCall.createFieldWithExternalPropertyValue(property: IrProperty) =
            context.irFactory.createField(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = EXTERNAL_SUPER_ACCESSORS_ORIGIN,
                name = Name.identifier("\$super_${property.getJsNameOrKotlinName().identifier}"),
                visibility = DescriptorVisibilities.PRIVATE,
                symbol = IrFieldSymbolImpl(),
                type = type,
                isFinal = true,
                isStatic = false,
                isExternal = false,
            ).apply {
                this.parent = irClass
                irClass.declarations.add(this)
            }

    }

    private data class ExternalPropertySuperAccess(
        var value: IrVariable,
        var field: IrField? = null
    )
}