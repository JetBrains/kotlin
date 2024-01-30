// MODULE: context

// FILE: context.kt
fun test() {
    fun call(a: Int, b: String) {
        println(b.repeat(a))
    }

    val x = 2
    val y = "foo"
    <caret_context>val z = Unit
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
call(x, y)