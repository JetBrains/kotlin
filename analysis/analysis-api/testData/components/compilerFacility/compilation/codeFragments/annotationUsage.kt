// MODULE: context

// FILE: lib.kt
package lib

@Target(AnnotationTarget.FUNCTION)
annotation class Anno(val x: String = "foo")

// FILE: context.kt
import lib.Anno

@Anno
fun foo(): Int = 5

@Anno(x = "bar")
fun bar(): Int = 10

fun test() {
    <caret_context>val x = 0
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
foo() + bar()