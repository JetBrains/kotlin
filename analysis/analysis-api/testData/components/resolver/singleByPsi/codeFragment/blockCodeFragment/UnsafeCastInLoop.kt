// IGNORE_FE10

// MODULE: context

// FILE: context.kt
fun test() {
    <caret_context>Unit
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
var num: Int? = 3
val numNotNull = num!!
for (x in 1..2) {
    val num2 = num
    num.<caret>inc()
    num = null
}
