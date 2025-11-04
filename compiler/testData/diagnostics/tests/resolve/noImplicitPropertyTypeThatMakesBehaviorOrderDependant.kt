// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-76240
// DIAGNOSTICS: -TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM -TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR -DEBUG_INFO_MISSING_UNRESOLVED

fun Int.lazyDecl() = toString()

class C1 {
    val lazyDecl by lazy { 42.lazyDecl() }
}

class C2 {
    fun test() = "str".extDecl()

    val String.extDecl
        get() = "Extension property in C2"
}

fun String.extDecl() = "Extension top-level function"

// FILE: withStdlib.kt

class A {
    val l = listOf("").map { it.length }
    val map = mutableMapOf<String, String>()
}

// FILE: noWarningOnSingleElementLoop.kt

val d = 42

object O1 {
    fun d(): Any = Any()
}

object O2 {
    // Consider several candidates during resolving including the containing property declaration.
    // It should not cause the warning since it's a single-element loop.
    val d = O1.d()
}

// FILE: loopAndUnrelatedProperty.kt

package loopAndUnrelatedProperty

fun String.f(): String = ""
fun bar() = A().foo()

class A {
    val map = mapOf<String, String>()

    fun foo() = run {
        bar()

        listOf<String>().map { it.length } // `map` has really simply computable type

        ""
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral, lambdaLiteral,
propertyDeclaration, propertyDelegate */
