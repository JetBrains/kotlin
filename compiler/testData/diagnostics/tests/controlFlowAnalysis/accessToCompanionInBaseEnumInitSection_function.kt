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
        val aInside = foo()
        val bInside = inPlaceRun { foo() }
        val cInside = nonInPlaceRun { foo() }

        val dInside by foo()
        val eInside by inPlaceDelegate { foo() }
        val fInside by nonInPlaceDelegate { foo() }
    },
    B {
        init {
            val aInit = foo()
            val bInit = inPlaceRun { foo() }
            val cInit = nonInPlaceRun { foo() }

            val dInit by foo()
            val eInit by inPlaceDelegate { foo() }
            val fInit by nonInPlaceDelegate { foo() }
        }
    },
    C {
        init {
            class Local {
                val aInside = foo()
                val bInside = inPlaceRun { foo() }
                val cInside = nonInPlaceRun { foo() }

                val dInside by foo()
                val eInside by inPlaceDelegate { foo() }
                val fInside by nonInPlaceDelegate { foo() }

                init {
                    val aInit = foo()
                    val bInit = inPlaceRun { foo() }
                    val cInit = nonInPlaceRun { foo() }

                    val dInit by foo()
                    val eInit by inPlaceDelegate { foo() }
                    val fInit by nonInPlaceDelegate { foo() }
                }

                fun localFun() {
                    val a = foo()
                    val b = inPlaceRun { foo() }
                    val c = nonInPlaceRun { foo() }

                    val d by foo()
                    val e by inPlaceDelegate { foo() }
                    val f by nonInPlaceDelegate { foo() }
                }
            }
        }
    },
    D {
        init {
            val someObj = object {
                val aInside = foo()
                val bInside = inPlaceRun { foo() }
                val cInside = nonInPlaceRun { foo() }

                val dInside by foo()
                val eInside by inPlaceDelegate { foo() }
                val fInside by nonInPlaceDelegate { foo() }

                init {
                    val aInit = foo()
                    val bInit = inPlaceRun { foo() }
                    val cInit = nonInPlaceRun { foo() }

                    val dInit by foo()
                    val eInit by inPlaceDelegate { foo() }
                    val fInit by nonInPlaceDelegate { foo() }
                }

                fun localFun() {
                    val a = foo()
                    val b = inPlaceRun { foo() }
                    val c = nonInPlaceRun { foo() }

                    val d by foo()
                    val e by inPlaceDelegate { foo() }
                    val f by nonInPlaceDelegate { foo() }
                }
            }
        }
    }
    ;

    val a = foo()
    val b = inPlaceRun { foo() }
    val c = nonInPlaceRun { foo() }

    val d by foo()
    val e by inPlaceDelegate { foo() }
    val f by nonInPlaceDelegate { foo() }

    companion object {
        fun foo(): String = "foo()"
    }
}

enum class EnumWithConstructor(val a: String, val b: String, val c: String) {
    A(
        a = <!UNINITIALIZED_ENUM_COMPANION!>foo()<!>,
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>foo()<!> },
        c = nonInPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>foo()<!> }
    );

    companion object {
        fun foo(): String = "foo()"
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
