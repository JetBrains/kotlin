// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
inline fun <T> foo(a: T)
        where T : CharSequence,
              T : Comparable<T> =
    a.length

fun <T> bar(a: T)
        where T : CharSequence,
              T : Comparable<T> =
    a.length

fun box(): String {
    val arguments = listOf<String>("0123456789", "", "\n")
    for (arg in arguments)
        if (foo(arg) != bar(arg))
            return arg
    return "OK"
}