// DUMP_CODE

// MODULE: context

// FILE: context.kt
const val prefix = "my"

class Context {
    @JvmName("${prefix}_foo")
    fun foo(): String = "foo"

    val bar: String
        @JvmName("${prefix}_getBar") get() = "bar"
}

fun test(context: Context) {
    <caret_context>Unit
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
context.foo() + context.bar