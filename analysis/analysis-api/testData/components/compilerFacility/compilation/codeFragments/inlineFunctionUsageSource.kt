// MODULE: context

// FILE: context.kt
fun test() {
    <caret_context>val x = 0
}

inline fun call(block: (Int) -> Int) {
    System.out.println(block(5))
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
call { it * 2 }