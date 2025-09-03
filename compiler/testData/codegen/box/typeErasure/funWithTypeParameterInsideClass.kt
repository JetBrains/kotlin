// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
class A {
    inline fun <T : CharSequence> foo(a: T) = a.length

    fun <T : CharSequence> bar(a: T) = a.length
}

fun box(): String {
    val arguments = listOf<String>("0123456789", "", "\n")
    for (arg in arguments)
        if (A().foo(arg) != A().bar(arg))
            return arg
    return "OK"
}