// MODULE: context

// FILE: context.kt
fun test() {
    block("foo") {
        fun call() {
            consume(this@block)
        }

        <caret_context>call()
    }
}

fun <T> block(obj: T, block: T.() -> Unit) {
    obj.block()
}

fun consume(text: String) {}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
call()