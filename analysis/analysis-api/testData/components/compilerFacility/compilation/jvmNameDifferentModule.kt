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


// MODULE: main(lib)
// LibrarySource-to-LibrarySource is currently not supported
// COMPILATION_ERRORS
// FILE: main.kt
fun test(context: Context) {
    context.foo() + context.bar
}