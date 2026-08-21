/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubclassOf
import org.jetbrains.kotlin.ir.util.parentAsClass

/**
 * Concrete value lattice for JS IR dataflow.
 *
 * Invariant: [join] is commutative, associative, and idempotent; [Bottom] is identity,
 * [Top] is annihilator.
 */
sealed interface JsValueLattice {
    object Top : JsValueLattice
    object Bottom : JsValueLattice

    data class Const(val kind: IrConstKind, val value: Any?) : JsValueLattice

    data class Enum(val entry: IrEnumEntry) : JsValueLattice {
        override fun toString(): String {
            val parentName = (entry.parent as? IrClass)?.name?.asString() ?: "?"
            return "$parentName.${entry.name.asString()}"
        }
    }

    object Unit : JsValueLattice

    fun isNull(): Boolean = this === Null

    fun asBooleanOrNull(): Boolean? =
        (this as? Const)?.takeIf { it.kind == IrConstKind.Boolean }?.value as? Boolean

    fun join(other: JsValueLattice): JsValueLattice = when {
        this is Bottom -> other
        other is Bottom -> this
        this is Top || other is Top -> Top
        this == other -> this
        else -> Top
    }

    companion object {
        val Null = Const(kind = IrConstKind.Null, value = null)
    }
}

/**
 * Concrete type lattice.
 *
 * - [Exact]: runtime class is known precisely; [Exact.nullable] tracks nullability.
 * - [UpperBound]: value is a subtype of [UpperBound.irClass] (can be an interface / open class).
 */
sealed interface JsTypeLattice {
    data object Top : JsTypeLattice
    data object Bottom : JsTypeLattice

    data class Exact(val irClass: IrClass, val nullable: Boolean) : JsTypeLattice {
        val isFinal: Boolean get() = irClass.modality == Modality.FINAL
    }

    data class UpperBound(val irClass: IrClass, val nullable: Boolean) : JsTypeLattice {
        val isFinal: Boolean get() = irClass.modality == Modality.FINAL
    }

    fun join(other: JsTypeLattice): JsTypeLattice = when {
        this is Bottom -> other
        other is Bottom -> this
        this is Top || other is Top -> Top
        this is Exact && other is Exact -> when {
            this.irClass == other.irClass -> Exact(irClass, nullable || other.nullable)
            else -> Top
        }
        this is Exact && other is UpperBound -> when {
            this.irClass == other.irClass || this.irClass.isSubclassOf(other.irClass) ->
                UpperBound(other.irClass, nullable || other.nullable)
            else -> Top
        }
        this is UpperBound && other is Exact -> other.join(this)
        this is UpperBound && other is UpperBound -> when {
            this.irClass == other.irClass -> UpperBound(irClass, nullable || other.nullable)
            this.irClass.isSubclassOf(other.irClass) -> UpperBound(other.irClass, nullable || other.nullable)
            other.irClass.isSubclassOf(this.irClass) -> UpperBound(this.irClass, nullable || other.nullable)
            else -> Top
        }
        else -> Top
    }

    companion object {
        fun exactOf(irClass: IrClass, nullable: Boolean = false): Exact = Exact(irClass, nullable)

        fun upperBoundOf(irClass: IrClass, nullable: Boolean = false): UpperBound =
            UpperBound(irClass, nullable)
    }
}

/**
 * Pair of value/type facts for a single IR value declaration.
 */
data class JsFact(
    val value: JsValueLattice = JsValueLattice.Bottom,
    val type: JsTypeLattice = JsTypeLattice.Bottom,
) {
    fun join(other: JsFact): JsFact = JsFact(value.join(other.value), type.join(other.type))

    companion object {
        val Top: JsFact = JsFact(JsValueLattice.Top, JsTypeLattice.Top)
        val Bottom: JsFact = JsFact(JsValueLattice.Bottom, JsTypeLattice.Bottom)
    }
}

/**
 * Sparse environment of facts for locals / parameters.
 */
class FactEnv(val map: MutableMap<IrValueDeclaration, JsFact> = mutableMapOf()) {
    operator fun get(value: IrValueDeclaration): JsFact? = map[value]
    operator fun set(value: IrValueDeclaration, fact: JsFact) {
        map[value] = fact
    }

    fun copy(): FactEnv = FactEnv(map.toMutableMap())

    fun join(other: FactEnv): FactEnv {
        val keys = map.keys + other.map.keys
        val result = FactEnv()
        for (key in keys) {
            val a = map[key] ?: JsFact.Bottom
            val b = other.map[key] ?: JsFact.Bottom
            result[key] = a.join(b)
        }
        return result
    }

    fun joinInPlace(other: FactEnv) {
        for (entry in other.map) {
            map[entry.key] = (map[entry.key] ?: JsFact.Bottom).join(entry.value)
        }
    }

    fun equivalentTo(other: FactEnv): Boolean {
        if (map.size != other.map.size) return false
        for (entry in map) {
            if (other.map[entry.key] != entry.value) return false
        }
        return true
    }

    companion object {
        val lattice: Lattice<FactEnv> = object : Lattice<FactEnv> {
            override fun bottom(): FactEnv = FactEnv()
            override fun join(a: FactEnv, b: FactEnv): FactEnv = a.join(b)
            override fun equivalent(a: FactEnv, b: FactEnv): Boolean = a.equivalentTo(b)
        }
    }
}

fun IrType.toFact(): JsFact {
    if (isUnit()) {
        return JsFact(value = JsValueLattice.Unit, type = JsTypeLattice.Top)
    }
    val irClass = classOrNull?.owner ?: return JsFact.Top
    val nullable = isNullable()
    val lattice = when {
        irClass.modality == Modality.FINAL && irClass.kind == ClassKind.CLASS ->
            JsTypeLattice.exactOf(irClass, nullable)
        else -> JsTypeLattice.upperBoundOf(irClass, nullable)
    }
    return JsFact(JsValueLattice.Top, lattice)
}

fun IrEnumEntry.toEnumFact(): JsFact =
    JsFact(
        value = JsValueLattice.Enum(entry = this),
        type = JsTypeLattice.exactOf(irClass = parentAsClass, nullable = false),
    )
