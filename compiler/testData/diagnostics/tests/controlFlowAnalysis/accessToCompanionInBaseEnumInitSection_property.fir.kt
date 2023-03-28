// DIAGNOSTICS: -UNUSED_VARIABLE
// IGNORE_REVERSED_RESOLVE
//  Ignore reason: KT-57619
// WITH_STDLIB
// ISSUE: KT-57456, KT-57608
@file:OptIn(ExperimentalContracts::class)

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

enum class Enum {
    A {
        val aInside = <!UNINITIALIZED_ENUM_COMPANION!>value<!>
        val bInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
        val cInside = nonInPlaceRun { value }

        val dInside by <!UNINITIALIZED_ENUM_COMPANION!>value<!>
        val eInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
        val fInside by nonInPlaceDelegate { value }
    },
    B {
        init {
            val aInit = <!UNINITIALIZED_ENUM_COMPANION!>value<!>
            val bInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
            val cInit = nonInPlaceRun { value }

            val dInit by <!UNINITIALIZED_ENUM_COMPANION!>value<!>
            val eInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
            val fInit by nonInPlaceDelegate { value }
        }
    },
    C {
        init {
            class Local {
                val aInside = value
                val bInside = inPlaceRun { value }
                val cInside = nonInPlaceRun { value }

                val dInside by value
                val eInside by inPlaceDelegate { value }
                val fInside by nonInPlaceDelegate { value }

                init {
                    val aInit = value
                    val bInit = inPlaceRun { value }
                    val cInit = nonInPlaceRun { value }

                    val dInit by value
                    val eInit by inPlaceDelegate { value }
                    val fInit by nonInPlaceDelegate { value }
                }

                fun localFun() {
                    val a = value
                    val b = inPlaceRun { value }
                    val c = nonInPlaceRun { value }

                    val d by value
                    val e by inPlaceDelegate { value }
                    val f by nonInPlaceDelegate { value }
                }
            }
        }
    },
    D {
        init {
            val someObj = object {
                val aInside = <!UNINITIALIZED_ENUM_COMPANION!>value<!>
                val bInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
                val cInside = nonInPlaceRun { value }

                val dInside by <!UNINITIALIZED_ENUM_COMPANION!>value<!>
                val eInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
                val fInside by nonInPlaceDelegate { value }

                init {
                    val aInit = <!UNINITIALIZED_ENUM_COMPANION!>value<!>
                    val bInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
                    val cInit = nonInPlaceRun { value }

                    val dInit by <!UNINITIALIZED_ENUM_COMPANION!>value<!>
                    val eInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
                    val fInit by nonInPlaceDelegate { value }
                }

                fun localFun() {
                    val a = value
                    val b = inPlaceRun { value }
                    val c = nonInPlaceRun { value }

                    val d by value
                    val e by inPlaceDelegate { value }
                    val f by nonInPlaceDelegate { value }
                }
            }
        }
    }
    ;

    val a = <!UNINITIALIZED_ENUM_COMPANION!>value<!>
    val b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
    val c = nonInPlaceRun { value }

    val d by <!UNINITIALIZED_ENUM_COMPANION!>value<!>
    val e by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
    val f by nonInPlaceDelegate { value }

    companion object {
        val value = "value"
    }
}

enum class EnumWithConstructor(val a: String, val b: String, val c: String) {
    A(
        a = <!UNINITIALIZED_ENUM_COMPANION!>value<!>,
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>value<!> },
        c = nonInPlaceRun { value }
    );

    companion object {
        val value = "value"
    }
}

operator fun <T> T.provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, T> = ReadOnlyProperty { _, _ -> this }

inline fun <T> inPlaceRun(block: () -> T): T {
    contract { callsInPlace(block) }
    return block()
}

fun <T> nonInPlaceRun(block: () -> T): T {
    return block()
}

inline fun <T> inPlaceDelegate(block: () -> T): ReadOnlyProperty<Any?, T> {
    contract { callsInPlace(block) }
    val value = block()
    return ReadOnlyProperty { _, _ -> value }
}

fun <T> nonInPlaceDelegate(block: () -> T): ReadOnlyProperty<Any?, T> {
    return ReadOnlyProperty { _, _ -> block() }
}
