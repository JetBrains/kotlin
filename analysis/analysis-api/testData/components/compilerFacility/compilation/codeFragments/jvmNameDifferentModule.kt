// DUMP_CODE

// MODULE: lib
// FILE: lib.kt
const val prefix = "my"

class Context {
    @JvmName("${prefix}_foo")
    fun foo(): String = "foo"

    val bar: String
        @JvmName("${prefix}_getBar") get() = "bar"
}


// MODULE: context(lib)
// LibrarySource-to-LibrarySource is currently not supported
// COMPILATION_ERRORS
// FILE: context.kt
fun test(context: Context) {
    <caret_context>context
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
context.foo() + context.bar