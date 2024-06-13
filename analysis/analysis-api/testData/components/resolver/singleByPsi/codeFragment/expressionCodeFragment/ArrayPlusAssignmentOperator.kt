// IGNORE_FE10

// MODULE: context

interface MyList {
    operator fun get(index: Int): String
    operator fun set(index: Int, value: String)
}

// FILE: context.kt
fun test(list: MyList) {
    <caret_context>Unit
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
list[10] <caret>+= "value"