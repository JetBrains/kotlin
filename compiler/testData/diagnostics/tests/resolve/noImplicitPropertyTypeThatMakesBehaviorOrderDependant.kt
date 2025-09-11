// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-76240

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

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral, lambdaLiteral,
propertyDeclaration, propertyDelegate */
