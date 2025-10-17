// DUMP_CODE

// MODULE: context

//FILE: context.kt
fun main() {
    <caret_context>call()
}

fun call(): Nothing {
    doNotCall()
}

fun doNotCall(): Nothing {
    error("Boo")
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
call()