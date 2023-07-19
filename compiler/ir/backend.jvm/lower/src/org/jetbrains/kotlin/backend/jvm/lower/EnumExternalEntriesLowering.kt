/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.findEnumValuesFunction
import org.jetbrains.kotlin.backend.jvm.ir.isEnumClassWhichRequiresExternalEntries
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal val enumExternalEntriesPhase = makeIrFilePhase(
    ::EnumExternalEntriesLowering,
    name = "EnumExternalEntries",
    description = "Replaces '.entries' on Java and pre-compiled Kotlin enums with access to entries in generated \$EntriesMapping "
)

/**
 * When this lowering encounters call to `Enum.entries` where `Enum` is either Java enum or enum pre-compiled
 * with previous version of Kotlin, it generates `FileName$EntriesMapping` where it stores
 * package-private `entries` static field that is used as a replacement of missing one.
 *
 * Basically, it lowers the following code:
 * ```
 * // F.kt
 * JavaOrOldKotlinEnum.entries
 * ```
 *
 * into
 * ```
 * synthetic class FKt$EntriesMappings {
 *    static final EnumEntries<JavaOrOldKotlinEnum> entries$1
 *    static {
 *        entries$1 = EnumEntries(JavaOrOldKotlinEnum::values)
 *    }
 * }
 *
 * // F.kt
 * FKt$EntriesMappings.entries$1
 * ```
 *
 * There's similar code which handles the `enumEntries<Enum>()` intrinsic code generation in `EnumEntriesIntrinsicMappingsCacheImpl`.
 */
class EnumExternalEntriesLowering(private val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {

    override fun lower(irFile: IrFile) {
        if (!context.state.languageVersionSettings.supportsFeature(LanguageFeature.EnumEntries)) {
            return
        }
        irFile.transformChildrenVoid(this)
    }

    private var state: EntriesMappingState? = null

    private inner class EntriesMappingState {
        val mappings = mutableMapOf<IrClass /* enum */, IrField>()
        val mappingsClass by lazy {
            context.irFactory.buildClass {
                name = Name.identifier("EntriesMappings")
                origin = JvmLoweredDeclarationOrigin.ENUM_MAPPINGS_FOR_ENTRIES
            }.apply {
                createImplicitParameterDeclarationWithWrappedDescriptor()
            }
        }

        fun getEntriesFieldForEnum(enumClass: IrClass): IrField {
            return mappings.getOrPut(enumClass) {
                mappingsClass.addField {
                    name = Name.identifier("entries\$${mappings.size}")
                    type = context.ir.symbols.enumEntries.typeWith(enumClass.defaultType)
                    origin = JvmLoweredDeclarationOrigin.ENUM_MAPPINGS_FOR_ENTRIES
                    isFinal = true
                    isStatic = true
                }
            }
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val owner = expression.symbol.owner as? IrSimpleFunction
        val parentClass = owner?.parent as? IrClass ?: return super.visitCall(expression)
        val shouldBeLowered = owner.name == SpecialNames.ENUM_GET_ENTRIES && parentClass.isEnumClassWhichRequiresExternalEntries()
        if (!shouldBeLowered) return super.visitCall(expression)
        val field = state!!.getEntriesFieldForEnum(parentClass)
        return IrGetFieldImpl(expression.startOffset, expression.endOffset, field.symbol, field.type)
    }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        val oldState = state
        val mappingState = EntriesMappingState()
        state = mappingState
        super.visitClassNew(declaration)

        for ((enum, field) in mappingState.mappings) {
            val enumValues = enum.findEnumValuesFunction(context)
            field.initializer =
                context.createIrBuilder(field.symbol).run {
                    irExprBody(
                        irCall(this@EnumExternalEntriesLowering.context.ir.symbols.createEnumEntries).apply {
                            putValueArgument(0, irCall(enumValues))
                        }
                    )
                }
        }

        if (mappingState.mappings.isNotEmpty()) {
            declaration.declarations += mappingState.mappingsClass.apply {
                parent = declaration
            }
        }
        state = oldState
        return declaration
    }
}
