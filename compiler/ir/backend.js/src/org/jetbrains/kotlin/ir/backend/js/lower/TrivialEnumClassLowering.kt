/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.getInstanceFun
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.isExported
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.findIsInstanceAnd
import kotlin.collections.plusAssign

internal val BOXES_CREATION_ORIGIN by IrStatementOriginImpl

/**
 * Whether the class was an enum class that has been lowered into an inline class by [TrivialEnumClassLowering].
 */
var IrClass.isLoweredTrivialEnum: Boolean by irFlag(copyByDefault = true)
var IrClass.namesContainer: IrField? by irAttribute(copyByDefault = true)
var IrClass.boxesContainer: IrField? by irAttribute(copyByDefault = true)
/**
 * Optimization: represents trivial enum classes as inline classes over their ordinal.
 *
 * A trivial enum class is an enum class that:
 *  - is not exported to JS
 *  - doesn't introduce new members into any of its enum entries (no enum entry has its own class body)
 *  - doesn't contain abstract members
 *  - has only a primary constructor whose parameters are only used to initialize at most one `val` property
 *  - there is no other mutable sates or mutable fields `enum entry`
 *
 * Before:
 * ```kotlin
 * enum class HttpCodes(val code: Int) {
 *   NOT_FOUND(404),
 *   FORBIDDEN(403),
 * }
 * ```
 *
 * After:
 * ```kotlin
 * inline class HttpCodes(override val ordinal: Int) : Enum<HttpCodes>(names[ordinal], ordinal) {
 *   override val name: String get() = names[ordinal]
 *   val code: Int get() = code$values[ordinal]
 *
 *   override fun equals(other: Any?): Boolean = other is HttpCodes && ordinal == other.ordinal
 *   override fun hashCode(): Int = ordinal
 *
 *   companion {
 *     private val names = arrayOf("NOT_FOUND", "FORBIDDEN")
 *     private val code$values = arrayOf(404, 403)
 *     private val boxes = arrayOf(boxed(0), boxed(1))
 *
 *     fun NOT_FOUND$getInstance(): HttpCodes = 0
 *     fun FORBIDDEN$getInstance(): HttpCodes = 1

 *     fun valueOf(name: String): HttpCodes {
 *        val ordinal = names.indexOf(name)
 *        if (ordinal == -1) throw IllegalArgumentException(...)
 *        return ordinal
 *     }
 *   }
 * }
 * ```
 *
 * The lowering works only on the declaration level. The usages of enum entries are lowered by [EnumUsageLowering],
 * and the `values`/`valueOf`/`entries` synthetic functions are lowered by [EnumSyntheticFunctionsAndPropertiesLowering].
 * It's disabled when incremental caches are used, because the enum representation is not stable across the module boundaries.
 */
class TrivialEnumClassLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    companion object {
        private const val NAMES_FIELD_NAME = "names"
        private const val BOXES_FIELD_NAME = "boxes"
        private const val PROPERTY_VALUES_FIELD_SUFFIX = "\$values"
    }

    private val irBuiltIns = context.irBuiltIns
    private val arrayGetFunction = irBuiltIns.arrayClass.getSimpleFunction("get")!!
    private val enumClass = irBuiltIns.enumClass.owner
    private val boxIntrinsic = context.symbols.jsBoxIntrinsic

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (context.incrementalCacheEnabled) return null
        if (declaration !is IrClass || !declaration.isTrivialEnum()) return null

        TrivialEnumTransformer(declaration).transform()

        return null
    }

    private fun IrClass.isTrivialEnum(): Boolean {
        if (!isEnumClass || isExpect || isEffectivelyExternal() || isExported(context)) return false

        // For the sake of simplicity we're optimizing enums with 0 or 1 value parameter in the constructor.
        // We can optimize if there are more parameters, but we need to consider keeping the effect order the same
        val primaryConstructor = primaryConstructor ?: return false
        if (primaryConstructor.parameters.size > 1 || primaryConstructor.parameters.any { param -> param.varargElementType == null }) {
            return false
        }

        var propertiesWithBackingFieldCount = 0
        for (member in declarations) {
            when (member) {
                is IrEnumEntry -> if (member.correspondingClass != null || member.initializerExpression?.expression !is IrEnumConstructorCall) return false
                is IrClass, is IrAnonymousInitializer -> return false
                is IrSimpleFunction -> if (member.modality == Modality.ABSTRACT) return false
                is IrProperty -> {
                    if (member.modality == Modality.ABSTRACT || member.isVar) return false
                    val backingField = member.backingField ?: continue
                    if (++propertiesWithBackingFieldCount > 1 || !backingField.primaryConstructorParameter) return false
                }
                is IrField -> return false
                else -> {}
            }
        }

        return true
    }

    private inner class TrivialEnumTransformer(private val irClass: IrClass) {
        private val enumEntries = irClass.declarations.filterIsInstance<IrEnumEntry>()
        private val otherPropertiesWithBackingFields = irClass.declarations
            .filterIsInstance<IrProperty>()
            .filter { !it.isFakeOverride && it.backingField != null }
        private val oldConstructor =
            irClass.primaryConstructor ?: compilationException("No primary constructor found for a enum class", irClass)

        private lateinit var ordinalField: IrField
        private lateinit var ordinalProperty: IrProperty
        private lateinit var namesField: IrField

        fun transform() {
            ordinalProperty = irClass.findFakeOverrideProperty("ordinal")
            val nameProperty = irClass.findFakeOverrideProperty("name")

            ordinalField = createOrdinalField(ordinalProperty)
            namesField = irClass.addStaticArrayField(
                NAMES_FIELD_NAME,
                irBuiltIns.stringType,
                enumEntries.map {
                    JsIrBuilder.buildString(context.irBuiltIns.stringType, it.name.identifier)
                }
            ).also { irClass.namesContainer = it }

            irClass.addStaticArrayField(
                BOXES_FIELD_NAME,
                irClass.defaultType,
                List(enumEntries.size) { index ->
                    context.createIrBuilder(irClass.symbol)
                        .irCall(boxIntrinsic.owner, origin = BOXES_CREATION_ORIGIN)
                        .apply {
                            typeArguments[0] = irClass.defaultType
                            arguments[0] = JsIrBuilder.buildInt(context.irBuiltIns.intType, index)
                        }
                }
            ).also { irClass.boxesContainer = it }

            otherPropertiesWithBackingFields.forEach(::transformOtherPropertiesWithBackingFields)

            ordinalProperty.overrideDeclarationWithFollowingReturn {
                irGetField(irGet(it.dispatchReceiverParameter!!), ordinalField)
            }

            nameProperty.overrideDeclarationWithFollowingReturn {
                irArrayGet(namesField, irGetOrdinal(it), irBuiltIns.stringType)
            }

            // The origin is used by `EqualityAndComparisonCallsTransformer` to compare the unboxed values with `===`
            irClass.findFakeOverrideFunction("equals")
                .overrideDeclarationWithFollowingReturn(IrDeclarationOrigin.GENERATED_INLINE_CLASS_MEMBER) {
                    createEqualsBody(it)
                }
            irClass.findFakeOverrideFunction("hashCode")
                .overrideDeclarationWithFollowingReturn(IrDeclarationOrigin.GENERATED_INLINE_CLASS_MEMBER) {
                    irGetOrdinal(it)
                }

            for ([ordinal, enumEntry] in enumEntries.withIndex()) {
                enumEntry.initializerExpression = null
                enumEntry.getInstanceFun = createGetInstanceFunction(enumEntry, ordinal)
                    .also { irClass.declarations += it }
            }

            irClass.declarations.remove(oldConstructor)
            irClass.declarations += createPrimaryConstructor()

            irClass.kind = ClassKind.CLASS
            irClass.modality = Modality.FINAL
            irClass.isValue = true
            irClass.valueClassRepresentation = InlineClassRepresentation(ordinalField.name, ordinalField.type as IrSimpleType)
            irClass.isLoweredTrivialEnum = true
        }

        private fun IrClass.findFakeOverrideProperty(name: String): IrProperty =
            declarations.findIsInstanceAnd<IrProperty> { it.isFakeOverride && it.name.asString() == name }
                ?: error("Enum class '${irClass.render()}' should have a fake override of the '$name' property")

        private fun IrClass.findFakeOverrideFunction(name: String): IrSimpleFunction =
            declarations.findIsInstanceAnd<IrSimpleFunction> { it.isFakeOverride && it.name.asString() == name }
                ?: error("Enum class '${irClass.render()}' should have a fake override of the '$name' function")

        private fun createOrdinalField(ordinalProperty: IrProperty): IrField =
            context.irFactory.buildField {
                name = ordinalProperty.name
                type = irBuiltIns.intType
                visibility = DescriptorVisibilities.PRIVATE
                isFinal = true
            }.apply {
                parent = irClass
                correspondingPropertySymbol = ordinalProperty.symbol
                ordinalProperty.backingField = this
            }

        private fun IrClass.addStaticArrayField(
            fieldName: String,
            elementType: IrType,
            elements: List<IrExpression>
        ): IrField {
            val arrayType = irBuiltIns.arrayClass.typeWith(elementType)
            return context.irFactory.buildField {
                name = Name.identifier(fieldName)
                type = arrayType
                visibility = DescriptorVisibilities.PRIVATE
                isFinal = true
                isStatic = true
            }.apply {
                parent = this@addStaticArrayField
                initializer = context.irFactory.createExpressionBody(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    IrVarargImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, arrayType, elementType, elements)
                ).also { it.patchDeclarationParents(this) }
                declarations += this
            }
        }

        private fun IrProperty.overrideDeclarationWithFollowingReturn(buildGetterResult: IrBuilderWithScope.(IrSimpleFunction) -> IrExpression) {
            isFakeOverride = false
            modality = Modality.FINAL
            getter!!.overrideDeclarationWithFollowingReturn(buildResult = buildGetterResult)
        }

        private fun IrSimpleFunction.overrideDeclarationWithFollowingReturn(
            newOrigin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED,
            buildResult: IrBuilderWithScope.(IrSimpleFunction) -> IrExpression,
        ) {
            isFakeOverride = false
            modality = Modality.FINAL
            origin = newOrigin
            dispatchReceiverParameter!!.type = irClass.defaultType
            body = context.createIrBuilder(symbol).irBlockBody(this) {
                +irReturn(buildResult(this@overrideDeclarationWithFollowingReturn))
            }
        }

        private fun IrBuilderWithScope.irGetOrdinal(function: IrSimpleFunction): IrExpression =
            irGetField(irGet(function.dispatchReceiverParameter!!), ordinalField)

        private fun IrBuilderWithScope.irArrayGet(arrayField: IrField, index: IrExpression, elementType: IrType): IrExpression =
            irCall(arrayGetFunction, elementType).apply {
                arguments[0] = irGetField(null, arrayField)
                arguments[1] = index
            }

        private fun IrBuilderWithScope.createEqualsBody(equalsFunction: IrSimpleFunction): IrExpression {
            val other = equalsFunction.parameters.single { it.kind == IrParameterKind.Regular }
            val otherOrdinal = irCall(ordinalProperty.getter!!).apply {
                dispatchReceiver = irImplicitCast(irGet(other), irClass.defaultType)
            }
            return irIfThenElse(
                irBuiltIns.booleanType,
                irIs(irGet(other), irClass.defaultType),
                irEquals(irGetOrdinal(equalsFunction), otherOrdinal),
                irFalse(),
                origin = IrStatementOrigin.ANDAND
            )
        }

        private fun transformOtherPropertiesWithBackingFields(property: IrProperty) {
            val backingField = property.backingField!!
            val initializer = backingField.initializer!!.expression
            val parameterIndex = ((initializer as? IrGetValue)?.symbol?.owner as? IrValueParameter)?.indexInParameters

            val valuesField = irClass.addStaticArrayField(
                property.name.asString() + PROPERTY_VALUES_FIELD_SUFFIX,
                backingField.type,
                enumEntries.map { enumEntry ->
                    val constructorCall = enumEntry.initializerExpression!!.expression as IrEnumConstructorCall
                    if (parameterIndex != null) {
                        constructorCall.arguments[parameterIndex]!!
                    } else {
                        initializer.deepCopyWithSymbols(irClass)
                    }
                }
            )

            property.backingField = null
            property.getter!!.let { getter ->
                getter.body = context.createIrBuilder(getter.symbol).irBlockBody(getter) {
                    +irReturn(irArrayGet(valuesField, irGetOrdinal(getter), backingField.type))
                }
            }
        }

        private fun createGetInstanceFunction(enumEntry: IrEnumEntry, ordinal: Int): IrSimpleFunction =
            context.irFactory.buildFun {
                name = Name.identifier("${enumEntry.name}$${ObjectDeclarationLowering.GET_INSTANCE_METHOD_NAME}")
                returnType = irClass.defaultType
                origin = JsLoweredDeclarationOrigin.ENUM_GET_INSTANCE_FUNCTION
            }.apply {
                parent = irClass
                body = context.createIrBuilder(symbol).irBlockBody(this) {
                    +irReturn(irReinterpretCast(irInt(ordinal), irClass.defaultType))
                }
            }

        private fun createPrimaryConstructor(): IrConstructor =
            context.irFactory.buildConstructor {
                startOffset = oldConstructor.startOffset
                endOffset = oldConstructor.endOffset
                origin = oldConstructor.origin
                visibility = oldConstructor.visibility
                returnType = irClass.defaultType
                isPrimary = true
            }.apply {
                parent = irClass
                val ordinalParameter = JsIrBuilder.buildValueParameter(this, ordinalField.name.asString(), irBuiltIns.intType)
                parameters = listOf(ordinalParameter)
                body = context.createIrBuilder(symbol).irBlockBody(this) {
                    +irDelegatingConstructorCall(enumClass.primaryConstructor!!).apply {
                        typeArguments[0] = irClass.defaultType
                        arguments[0] = irArrayGet(namesField, irGet(ordinalParameter), irBuiltIns.stringType)
                        arguments[1] = irGet(ordinalParameter)
                    }
                    +irSetField(irGet(irClass.thisReceiver!!), ordinalField, irGet(ordinalParameter))
                }
            }
    }

}

private val IrField.primaryConstructorParameter: Boolean
    get() = (initializer?.expression as? IrGetValue)?.origin == IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER
