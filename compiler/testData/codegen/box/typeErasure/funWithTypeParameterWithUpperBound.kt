// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
// FILE: lib.kt
inline fun <T : Number> foo(a: T) = a.toString()

// FILE: main.kt
fun <T : Number> bar(a: T) = a.toString()

fun box(): String {
    val arguments = listOf<Number>(42, 4.20, -0, 1L, 0x0F, 0b00001011, 123.5e10, 123.5f, 1_000_000)
    for (arg in arguments)
        if (foo(arg) != bar(arg))
            return arg.toString()
    return "OK"
}