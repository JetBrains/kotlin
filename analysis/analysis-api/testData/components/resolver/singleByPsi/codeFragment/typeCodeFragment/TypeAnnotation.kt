// IGNORE_FE10

// MODULE: context

// FILE: context.kt
@Target(AnnotationTarget.TYPE)
annotation class Anno

fun test() {
    <caret_context>Unit
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: TYPE
@<caret>Anno String