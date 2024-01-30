// IGNORE_FE10

// MODULE: context

// FILE: context.kt
class Foo

fun test() {
    <caret_context>Foo()
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: TYPE
<caret>Foo