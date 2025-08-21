// MODULE: context

//FILE: context.kt
fun main() {
    val localObject = object {
        fun foo() {}
    }
    <caret_context>val a = 1
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
localObject.foo()