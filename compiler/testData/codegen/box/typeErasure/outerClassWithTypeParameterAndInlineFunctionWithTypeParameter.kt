// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
class A<T>(val a: T) {
    inline fun <F> foo(b: T, c: F) = a.toString() + b.toString() + c.toString()

    fun <F> bar(b: T, c: F) = a.toString() + b.toString() + c.toString()
}

fun box(): String {
    val arguments = listOf<Any?>("0123456789", 4.20, true, null, A(null), A(A("")))
    for (arg in arguments)
        if (A(arg).foo(arg, arg) != A(arg).bar(arg, arg))
            return arg.toString()
    return "OK"
}