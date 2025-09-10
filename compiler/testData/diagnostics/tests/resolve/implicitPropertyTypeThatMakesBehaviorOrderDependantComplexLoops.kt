// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76240
// IGNORE_REVERSED_RESOLVE
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS -DEBUG_INFO_MISSING_UNRESOLVED

// FILE: loop.kt

package packageLoop

fun Int.f(): String = "ext func f"

class Foo(val b1: Boolean, val b2: Boolean) {
    fun g() = f()
    fun f() = if (b1) 42.f() else true
    val f = if (b2) f() else false
}

class Bar(val b1: Boolean, val b2: Boolean) {
    fun g() = f()
    val f = if (b2) f() else false
    fun f() = if (b1) 42.f() else true
}

// FILE: twoLoops.kt

package packageTwoLoops

fun Int.f(): String = "ext func f"
fun Int.g(): String = "ext func g"

class Foo(val b1: Boolean, val b2: Boolean) {
    fun f() = if (b1) 12.f() else 34.g()
    val f = if (b2) f() else g()

    fun g() = if (b1) 56.g() else 78.f()
    val g = if (b2) g() else <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR!>f()<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, ifExpression, integerLiteral,
primaryConstructor, propertyDeclaration, stringLiteral */
