// MODULE: context

// FILE: context.kt
class Foo {
    private val a: String = "foo"

    inner class Bar {
        private val b: String = "bar"

        fun test() {
            <caret_context>val x = 0
        }
    }
}

fun callFoo(foo: Foo): Int {
    return 0
}

fun callString(string: String): Int {
    return 1
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
callFoo(this@Foo) + callString(a) + callString(b)