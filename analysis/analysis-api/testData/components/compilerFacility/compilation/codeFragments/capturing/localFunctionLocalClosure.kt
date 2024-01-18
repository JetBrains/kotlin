// MODULE: context

// FILE: context.kt
fun test() {
    val x = 0

    fun call() {
        consume(x)
    }

    <caret_context>call()
}

fun consume(n: Int) {}


// MODULE: main
// MODULE_KIND: CodeFragment

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
call()