// MODULE: context
// ISSUE: KT-68695

// FILE: context.kt

class Clazz() {
    private lateinit var lateinitStr: String

    fun showLateinitStr() {
        <caret_context>lateinitStr
    }
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
this::lateinitStr
