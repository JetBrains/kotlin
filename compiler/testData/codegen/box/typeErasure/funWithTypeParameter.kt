// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB

inline fun <T> foo(a: T) = a.toString()

fun <T> bar(a: T) = a.toString()

fun box(): String {
    val arguments = listOf<Any?>("0123456789", 4.20, true, null)
    for (arg in arguments)
        if (foo(arg) != bar(arg))
            return arg.toString()
    return "OK"
}