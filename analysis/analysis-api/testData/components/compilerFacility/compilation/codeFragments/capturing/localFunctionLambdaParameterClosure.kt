// MODULE: context

// FILE: context.kt
fun test() {
    block("foo") { foo ->
        fun call() {
            consume(foo)
        }

        <caret_context>call()
    }
}

fun <T> block(obj: T, block: (T) -> Unit) {
    block(obj)
}

fun consume(text: String) {}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
call()