// MODULE: context

// FILE: context.kt
const val prefix = "prefix_"

@JvmName("${prefix}f1")
fun f1() = 42

var prop: Int = 0
    @JvmName("${prefix}getter")
    get() {
        return field + 1
    }
    @JvmName("${prefix}setter")
    set(value) {
        field = value + 1
    }

fun main() {
    <caret_context>val x = 1
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
f1()