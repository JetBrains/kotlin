// MODULE: context

// FILE: context.kt
fun test() {
    <caret_context>val x = 0
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
listOf(1, 2, 3, 4, 5)
    .filter { (it % 2) == 0 }
    .map { it * 2 }
    .forEach { System.out.println(it) }