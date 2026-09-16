// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// ALLOW_KOTLIN_PACKAGE
// FILE: RichError.kt
package kotlin

abstract class RichError
// FILE: test.kt
fun interface Sam {
    fun invoke()
}

<!WRONG_MODIFIER_TARGET!>error<!> class Foo

fun foo(f: Sam | Foo) { }

fun test() {
    foo {}
}

/* GENERATED_FIR_TAGS: classDeclaration, funInterface, functionDeclaration, interfaceDeclaration, lambdaLiteral,
samConversion */
