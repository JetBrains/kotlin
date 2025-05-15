// MODULE: context

//FILE: context.kt
fun main() {
    class Foo {
        inline fun foo() = object : () -> String {
            override fun invoke(): String {
                return "Foo"
            }
        }

        inline val bar: String
            get() = "Bar"
    }

    <caret_context>println("hi there")
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
Foo().foo()() + Foo().bar