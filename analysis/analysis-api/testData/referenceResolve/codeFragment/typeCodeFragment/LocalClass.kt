// IGNORE_FE10

// MODULE: context

// FILE: context.kt
fun test() {
    class Local {}

    <caret_context>Local()
}


// MODULE: main
// MODULE_KIND: CodeFragment

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: TYPE
<caret>Local