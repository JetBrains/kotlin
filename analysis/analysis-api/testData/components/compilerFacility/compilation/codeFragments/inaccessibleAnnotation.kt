// ISSUE: KT-71907

// MODULE: lib
annotation class Ann

// MODULE: app(lib)
// COMPILATION_ERRORS
// ^LibrarySource mode doesn't support module dependencies

@Ann
class Some {
    val x = "OK"
}

// MODULE: context(app)
// FILE: context.kt

fun test() {
    val some = Some()
    <caret_context>some.x
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
some.x
