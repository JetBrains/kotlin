// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// WITH_STDLIB
// ISSUE: KT-57456, KT-57608, KT-74926
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

        val gInside = <!UNINITIALIZED_ENUM_COMPANION_REFERENCE!>Companion<!>
        val hInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION_REFERENCE!>Companion<!> }
        val iInside = nonInPlaceRun { Companion }

        val jInside by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
        val kInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION_REFERENCE!>Companion<!> }
        val lInside by nonInPlaceDelegate { Companion }
    },
    B {
        init {
            val aInit = <!UNINITIALIZED_ENUM_COMPANION!>value<!>
            val bInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
            val cInit = nonInPlaceRun { value }

            val dInit by <!UNINITIALIZED_ENUM_COMPANION!>value<!>
            val eInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
            val fInit by nonInPlaceDelegate { value }

            val gInit = <!UNINITIALIZED_ENUM_COMPANION_REFERENCE!>Companion<!>
            val hInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION_REFERENCE!>Companion<!> }
            val iInit = nonInPlaceRun { Companion }

            val jInit by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
            val kInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION_REFERENCE!>Companion<!> }
            val lInit by nonInPlaceDelegate { Companion }
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

                val gInside = Companion
                val hInside = inPlaceRun { Companion }
                val iInside = nonInPlaceRun { Companion }

                val jInside by Companion
                val kInside by inPlaceDelegate { Companion }
                val lInside by nonInPlaceDelegate { Companion }

                init {
                    val aInit = value
                    val bInit = inPlaceRun { value }
                    val cInit = nonInPlaceRun { value }

                    val dInit by value
                    val eInit by inPlaceDelegate { value }
                    val fInit by nonInPlaceDelegate { value }

                    val gInit = Companion
                    val hInit = inPlaceRun { Companion }
                    val iInit = nonInPlaceRun { Companion }

                    val jInit by Companion
                    val kInit by inPlaceDelegate { Companion }
                    val lInit by nonInPlaceDelegate { Companion }
                }

                fun localFun() {
                    val a = value
                    val b = inPlaceRun { value }
                    val c = nonInPlaceRun { value }

                    val d by value
                    val e by inPlaceDelegate { value }
                    val f by nonInPlaceDelegate { value }

                    val g = Companion
                    val h = inPlaceRun { Companion }
                    val i = nonInPlaceRun { Companion }

                    val j by Companion
                    val k by inPlaceDelegate { Companion }
                    val l by nonInPlaceDelegate { Companion }
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

                val gInside = <!UNINITIALIZED_ENUM_COMPANION_REFERENCE!>Companion<!>
                val hInside = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION_REFERENCE!>Companion<!> }
                val iInside = nonInPlaceRun { Companion }

                val jInside by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
                val kInside by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION_REFERENCE!>Companion<!> }
                val lInside by nonInPlaceDelegate { Companion }

                init {
                    val aInit = <!UNINITIALIZED_ENUM_COMPANION!>value<!>
                    val bInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
                    val cInit = nonInPlaceRun { value }

                    val dInit by <!UNINITIALIZED_ENUM_COMPANION!>value<!>
                    val eInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION!>value<!> }
                    val fInit by nonInPlaceDelegate { value }

                    val gInit = <!UNINITIALIZED_ENUM_COMPANION_REFERENCE!>Companion<!>
                    val hInit = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION_REFERENCE!>Companion<!> }
                    val iInit = nonInPlaceRun { Companion }

                    val jInit by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
                    val kInit by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION_REFERENCE!>Companion<!> }
                    val lInit by nonInPlaceDelegate { Companion }
                }

                fun localFun() {
                    val a = value
                    val b = inPlaceRun { value }
                    val c = nonInPlaceRun { value }

                    val d by value
                    val e by inPlaceDelegate { value }
                    val f by nonInPlaceDelegate { value }

                    val g = Companion
                    val h = inPlaceRun { Companion }
                    val i = nonInPlaceRun { Companion }

                    val j by Companion
                    val k by inPlaceDelegate { Companion }
                    val l by nonInPlaceDelegate { Companion }
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

    val g = <!UNINITIALIZED_ENUM_COMPANION_REFERENCE!>Companion<!>
    val h = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION_REFERENCE!>Companion<!> }
    val i = nonInPlaceRun { Companion }

    val j by <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>
    val k by inPlaceDelegate { <!UNINITIALIZED_ENUM_COMPANION_REFERENCE!>Companion<!> }
    val l by nonInPlaceDelegate { Companion }

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

enum class EnumWithCompanionReferencesInConstructor(val a: Any, val b: Any, val c: Any) {
    A(
        a = <!UNINITIALIZED_ENUM_COMPANION_REFERENCE!>Companion<!>,
        b = inPlaceRun { <!UNINITIALIZED_ENUM_COMPANION_REFERENCE!>Companion<!> },
        c = nonInPlaceRun { Companion }
    );

    companion object
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

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, companionObject, contractCallsEffect, contracts,
enumDeclaration, enumEntry, funWithExtensionReceiver, functionDeclaration, functionalType, inline, lambdaLiteral,
localProperty, nullableType, objectDeclaration, operator, primaryConstructor, propertyDeclaration, propertyDelegate,
starProjection, stringLiteral, thisExpression, typeParameter */
