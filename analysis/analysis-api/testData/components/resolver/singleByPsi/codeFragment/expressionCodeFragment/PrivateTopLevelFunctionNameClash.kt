// MODULE: context
// ISSUE: KT-70287

// FILE: Alfa.kt
fun main() {
    <caret_context>Unit
}

private var pv: String = "alfa pv"

private fun foo(): String = "alfa foo"

// FILE: Bravo.kt
private var pv: Int = 42

private fun foo(): Int = 42

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
<caret>foo()
