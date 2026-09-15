// LANGUAGE: +ExplicitBackingFields

// MODULE: context

// FILE: context.kt
class Foo {
    val prop: List<Int>
        field: MutableList<Int> = run {
            fun call(a: Int): Int = a + 1

            val x = 2
            <caret_context>val z = Unit
            mutableListOf(x)
        }
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
call(x)
