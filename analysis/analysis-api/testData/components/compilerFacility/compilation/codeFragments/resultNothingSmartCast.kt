// DUMP_CODE

// MODULE: context

//FILE: context.kt
fun main() {
    var str: String? = null
    str = "not null"
    println(str)
    str = null
    <caret_context>println(str)
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
str