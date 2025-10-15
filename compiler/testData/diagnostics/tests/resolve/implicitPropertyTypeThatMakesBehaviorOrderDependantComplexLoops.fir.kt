// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76240
// IGNORE_REVERSED_RESOLVE
// IGNORE_PARTIAL_BODY_ANALYSIS
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS -DEBUG_INFO_MISSING_UNRESOLVED

// FILE: loop.kt

package packageLoop

fun Int.f(): String = "ext func f"

class Foo(val b1: Boolean, val b2: Boolean) {
    fun g() = f()
    fun f() = if (b1) 42.<!IMPLICIT_PROPERTY_TYPE_MAKES_BEHAVIOR_ORDER_DEPENDANT!>f<!>() else true
    val f = if (b2) <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>f()<!> else false
}

class Bar(val b1: Boolean, val b2: Boolean) {
    fun g() = f()
    val f = if (b2) <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>f()<!> else false
    fun f() = if (b1) 42.<!IMPLICIT_PROPERTY_TYPE_MAKES_BEHAVIOR_ORDER_DEPENDANT!>f<!>() else true
}

// FILE: twoLoops.kt

package packageTwoLoops

fun Int.f(): String = "ext func f"
fun Int.g(): String = "ext func g"

class Foo(val b1: Boolean, val b2: Boolean) {
    fun f() = if (b1) 12.<!IMPLICIT_PROPERTY_TYPE_MAKES_BEHAVIOR_ORDER_DEPENDANT!>f<!>() else 34.<!IMPLICIT_PROPERTY_TYPE_MAKES_BEHAVIOR_ORDER_DEPENDANT!>g<!>()
    val f = if (b2) <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>f()<!> else g()

    fun g() = if (b1) 56.<!IMPLICIT_PROPERTY_TYPE_MAKES_BEHAVIOR_ORDER_DEPENDANT!>g<!>() else 78.<!IMPLICIT_PROPERTY_TYPE_MAKES_BEHAVIOR_ORDER_DEPENDANT!>f<!>()
    val g = if (b2) <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>g()<!> else <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>f()<!>
}

// FILE: localFunction.kt

package localFunction

fun String.foo() = ""

class MyClass {
    fun foo() = run {
        fun localFun() {
            "".<!IMPLICIT_PROPERTY_TYPE_MAKES_BEHAVIOR_ORDER_DEPENDANT, IMPLICIT_PROPERTY_TYPE_MAKES_BEHAVIOR_ORDER_DEPENDANT!>foo<!>()
        }
        ""
    }

    val foo = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>foo()<!>
}

// FILE: localProperty.kt

package localProperty

fun String.foo() = ""

class MyClass {
    val foo = foo()

    fun foo() = run {
        val localProp = "".<!IMPLICIT_PROPERTY_TYPE_MAKES_BEHAVIOR_ORDER_DEPENDANT, IMPLICIT_PROPERTY_TYPE_MAKES_BEHAVIOR_ORDER_DEPENDANT!>foo<!>()
        ""
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, ifExpression, integerLiteral,
primaryConstructor, propertyDeclaration, stringLiteral */
