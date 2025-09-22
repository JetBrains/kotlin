// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
class A<T>(val a: T) where T : CharSequence, T : Comparable<T> {
    inline fun foo(b: T) = a.length + b.length

    fun bar(b: T) = a.length + b.length
}

fun box(): String {
    val arguments = listOf<String>("0123456789", "", "\n")
    for (arg in arguments)
        if (A(arg).foo(arg) != A(arg).bar(arg))
            return arg
    return "OK"
}