// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
inline fun <T : CharSequence> Int.foo(a: T) = a.length + this

fun <T : CharSequence> Int.bar(a: T) = a.length + this

fun box(): String {
    val arguments = listOf<String>("0123456789", "", "\n")
    for (arg in arguments)
        if (42.foo(arg) != 42.bar(arg))
            return arg
    return "OK"
}