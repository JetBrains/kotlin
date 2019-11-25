/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.EnumWhenLowering
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

internal val enumWhenPhase = makeIrFilePhase(
    ::MappedEnumWhenLowering,
    name = "EnumWhenLowering",
    description = "Replace `when` subjects of enum types with their ordinals"
)

// A version of EnumWhenLowering that is more friendly to incremental compilation. For example,
// suppose the code initially looks like this:
//
//     // 1.kt
//     enum E { X }
//
//     // 2.kt
//     fun f(e: E) = when (e) { E.X -> 1 }
//
// EnumWhenLowering would transform 2.kt into this:
//
//     fun f(e: E) = when (e.ordinal()) { 0 -> 1 }
//
// While this lowering would generate (approximately) this instead:
//
//     fun f(e: E) = when (WhenMappings.$EnumSwitchMapping$0[e.ordinal()]) { 1 -> 1 }
//
//     object WhenMappings {
//         // Note the runtime call to ordinal(): 0 is not hardcoded.
//         val $EnumSwitchMapping$0 = IntArray(E.values().size).also { it[E.X.ordinal()] = 1 }
//     }
//
// The latter would not need to be recompiled if new entries were added before `X`
// at the negligible cost of an additional initializer per run + one array read per call.
//
private class MappedEnumWhenLowering(context: CommonBackendContext) : EnumWhenLowering(context) {
    private val intArray = context.irBuiltIns.primitiveArrayForType.getValue(context.irBuiltIns.intType)
    private val intArrayConstructor = intArray.constructors.single { it.owner.valueParameters.size == 1 }
    private val intArrayGet = intArray.functions.single { it.owner.name == OperatorNameConventions.GET }
    private val intArraySet = intArray.functions.single { it.owner.name == OperatorNameConventions.SET }
    private val refArraySize = context.irBuiltIns.arrayClass.owner.properties.single { it.name.toString() == "size" }.getter!!

    // To avoid visibility-related issues, classes containing the mappings are direct children
    // of the classes in which they are used. This field tracks which container is the innermost one.
    private var state: EnumMappingState? = null

    private inner class EnumMappingState {
        val mappings = mutableMapOf<IrClass /* enum */, Pair<MutableMap<IrEnumEntry, Int>, IrField>>()
        val mappingsClass by lazy {
            buildClass {
                name = Name.identifier("WhenMappings")
                origin = JvmLoweredDeclarationOrigin.ENUM_MAPPINGS_FOR_WHEN
            }.apply {
                createImplicitParameterDeclarationWithWrappedDescriptor()
            }
        }

        fun getMappingForClass(enumClass: IrClass): Pair<MutableMap<IrEnumEntry, Int>, IrField> =
            mappings.getOrPut(enumClass) {
                mutableMapOf<IrEnumEntry, Int>() to mappingsClass.addField {
                    name = Name.identifier("\$EnumSwitchMapping\$${mappings.size}")
                    type = intArray.defaultType
                    origin = JvmLoweredDeclarationOrigin.ENUM_MAPPINGS_FOR_WHEN
                    isFinal = true
                    isStatic = true
                }
            }
    }

    override fun mapConstEnumEntry(entry: IrEnumEntry): Int {
        val (mapping) = state!!.getMappingForClass(entry.parentAsClass)
        // Index 0 (default value for integers) is reserved for unknown ordinals.
        return mapping.getOrPut(entry) { mapping.size + 1 }
    }

    override fun mapRuntimeEnumEntry(builder: IrBuilderWithScope, subject: IrExpression): IrExpression =
        builder.irCall(intArrayGet).apply {
            val (_, field) = state!!.getMappingForClass(subject.type.getClass()!!)
            dispatchReceiver = builder.irGetField(null, field)
            putValueArgument(0, super.mapRuntimeEnumEntry(builder, subject))
        }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        val oldState = state
        state = EnumMappingState()
        super.visitClassNew(declaration)

        for ((enum, mappingAndField) in state!!.mappings) {
            val (mapping, field) = mappingAndField
            val builder = context.createIrBuilder(state!!.mappingsClass.symbol)
            val enumValues = enum.functions.single { it.name.toString() == "values" }
            field.initializer = builder.irExprBody(builder.irBlock {
                val enumSize = irCall(refArraySize).apply { dispatchReceiver = irCall(enumValues) }
                val result = irTemporary(irCall(intArrayConstructor).apply { putValueArgument(0, enumSize) })
                for ((entry, index) in mapping) {
                    val runtimeEntry = IrGetEnumValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, enum.defaultType, entry.symbol)
                    +irCall(intArraySet).apply {
                        dispatchReceiver = irGet(result)
                        putValueArgument(0, super.mapRuntimeEnumEntry(builder, runtimeEntry)) // <entry>.ordinal()
                        putValueArgument(1, irInt(index))
                    }
                }
                +irGet(result)
            })
        }

        if (state!!.mappings.isNotEmpty()) {
            declaration.declarations += state!!.mappingsClass.apply { parent = declaration }
        }
        state = oldState
        return declaration
    }
}
