// ISSUE: KT-71263

// MODULE: context

// FILE: context.kt

class Aaa {
    fun foo(): String = "Hello"
}

val Aaa.xxx: String get() {
    return foo() + "x"
}

val Aaa.boo: String get() {
    <caret_context>return foo() + "boo"
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
xxx
