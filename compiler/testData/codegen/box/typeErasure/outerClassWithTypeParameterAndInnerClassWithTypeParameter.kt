// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
class A<T : CharSequence>(val a: T) {
    inner class Inner<I : CharSequence>(val b: T, val c: I) {
        inline fun foo(d: T, e: I) = a.length +
                b.length + c.length +
                d.length + e.length

        fun bar(d: T, e: I) = a.length +
                b.length + c.length +
                d.length + e.length
    }
}

fun box(): String {
    val arguments = listOf<String>("0123456789", "", "\n")
    for (arg in arguments)
        if (A(arg).Inner("0", "01").foo("012", "0123") != A(arg).Inner("0", "01").bar("012", "0123"))
            return arg
    return "OK"
}