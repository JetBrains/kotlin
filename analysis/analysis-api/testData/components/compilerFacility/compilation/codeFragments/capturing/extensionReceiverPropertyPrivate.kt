// MODULE: context
// ISSUE: KT-70824

// FILE: context.kt

fun test() {
    val o = Any()
    <caret_context>foo("${o.extensionProp}") // Breakpoint!
}

private val Any.extensionProp: Boolean
    get() = true

fun foo(s: String) {}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
o.extensionProp
