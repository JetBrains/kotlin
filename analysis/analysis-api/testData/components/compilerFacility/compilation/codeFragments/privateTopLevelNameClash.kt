// MODULE: context
// ISSUE: KT-70287

// FILE: Alfa.kt
fun main() {
    println(foo())
    <caret_context>println(pv)
}

private var pv = "alfa pv"

private fun foo() = "alfa foo"

// FILE: Bravo.kt
private var pv = "bravo pv"

private fun foo() = "bravo foo"

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
foo() + pv
