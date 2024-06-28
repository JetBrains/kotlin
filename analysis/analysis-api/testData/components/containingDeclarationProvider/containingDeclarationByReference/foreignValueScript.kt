// MODULE: lib

// FILE: lib.kt
fun call(text: String) {}


// MODULE: context(lib)

// FILE: context.kts
<caret_context>call("foo")


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
// CODE_FRAGMENT_FOREIGN_VALUE: foo_DebugLabel(Ljava/lang/String;)
<caret>foo_DebugLabel