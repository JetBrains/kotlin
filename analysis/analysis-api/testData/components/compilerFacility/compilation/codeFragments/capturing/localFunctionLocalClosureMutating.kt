// MODULE: context

// FILE: context.kt
fun test() {
    var x = 0

    fun call() {
        x = 1
    }

    <caret_context>call()
}


// MODULE: main
// MODULE_KIND: CodeFragment

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
call()