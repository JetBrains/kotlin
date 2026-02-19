// MODULE: context

//FILE: context.kt
fun main() {
    val obj = object {
        fun foo(): String {
            <caret_context>return "foo"
        }
    }
    obj.foo()
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
this