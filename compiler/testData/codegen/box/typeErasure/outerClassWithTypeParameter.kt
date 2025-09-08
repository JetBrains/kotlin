// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
class A<T>(val a: T) {
    inline fun foo(b: T) = a.toString() + b.toString()

    fun bar(b: T) = a.toString() + b.toString()
}

fun box(): String {
    val arguments = listOf<Any?>("0123456789", 4.20, true, null, A(null), A(A("")))
    for (arg in arguments)
        if (A(arg).foo(arg) != A(arg).bar(arg))
            return arg.toString()
    return "OK"
}