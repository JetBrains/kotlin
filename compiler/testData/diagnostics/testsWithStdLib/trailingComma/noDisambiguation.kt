// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_PARAMETER

fun foo(vararg x: Int) = false
fun foo() = true

fun main() {
    val x = foo()
    val y = foo(<!SYNTAX!><!>,)
}

/* GENERATED_FIR_TAGS: functionDeclaration, localProperty, propertyDeclaration, vararg */
