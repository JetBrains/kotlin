/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.wasm.utils.getWasmOpAnnotation
import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irLong
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.wasm.ir.WasmOp

/**
 * Associative operators the Wasm backend can use for accumulator transformation,
 * each paired with its identity element. The carrier type is the receiver and
 * return type of the operator.
 */
internal object WasmMonoids {
    @ConsistentCopyVisibility
    data class Monoid private constructor(val operator: String, val carrier: ClassId) {
        companion object {
            fun byInstruction(op: WasmOp, carrier: ClassId) = Monoid(op.name, carrier)
            fun byMember(name: Name, carrier: ClassId) = Monoid(name.asString(), carrier)
        }
    }

    val identities: Map<Monoid, IrBuilder.() -> IrExpression> = mapOf(
        Monoid.byInstruction(WasmOp.I32_ADD, StandardClassIds.Int) to { irInt(0) },
        Monoid.byInstruction(WasmOp.I32_MUL, StandardClassIds.Int) to { irInt(1) },
        Monoid.byInstruction(WasmOp.I32_AND, StandardClassIds.Int) to { irInt(-1) },
        Monoid.byInstruction(WasmOp.I32_OR, StandardClassIds.Int) to { irInt(0) },
        Monoid.byInstruction(WasmOp.I32_XOR, StandardClassIds.Int) to { irInt(0) },
        Monoid.byInstruction(WasmOp.I64_ADD, StandardClassIds.Long) to { irLong(0) },
        Monoid.byInstruction(WasmOp.I64_MUL, StandardClassIds.Long) to { irLong(1) },
        Monoid.byInstruction(WasmOp.I64_AND, StandardClassIds.Long) to { irLong(-1) },
        Monoid.byInstruction(WasmOp.I64_OR, StandardClassIds.Long) to { irLong(0) },
        Monoid.byInstruction(WasmOp.I64_XOR, StandardClassIds.Long) to { irLong(0) },
        Monoid.byInstruction(WasmOp.I32_AND, StandardClassIds.Boolean) to { irBoolean(true) },
        Monoid.byInstruction(WasmOp.I32_OR, StandardClassIds.Boolean) to { irBoolean(false) },
        Monoid.byInstruction(WasmOp.I32_XOR, StandardClassIds.Boolean) to { irBoolean(false) },
        Monoid.byMember(OperatorNameConventions.PLUS, StandardClassIds.String) to { irString("") },
    )

    fun of(call: IrCall): Monoid? {
        val owner = call.symbol.owner
        if (owner.parameters.size != 2) return null
        val carrierType = owner.returnType
        if (owner.parameters[0].type != carrierType) return null
        val carrier = carrierType.classOrNull?.owner?.classId ?: return null
        // Primitive operators are identified by their @WasmOp instruction name,
        // which the inline-class lowering carries over to the static -impl function.
        // String.plus is identified by its member name on the String class.
        val monoid = owner.getWasmOpAnnotation()?.let { Monoid.byInstruction(WasmOp.valueOf(it), carrier) }
            ?: Monoid.byMember(owner.name, carrier).takeIf { (owner.parent as? IrClass)?.classId == carrier }
            ?: return null
        return monoid.takeIf { it in identities }
    }
}
