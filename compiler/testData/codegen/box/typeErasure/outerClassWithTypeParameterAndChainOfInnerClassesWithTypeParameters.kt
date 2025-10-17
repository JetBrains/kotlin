// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
class A<T : CharSequence>(val a: T) {
    inner class Inner1<I1 : CharSequence>(val b: T, val c: I1) {
        inner class Inner2<I2 : CharSequence>(val d: T, val e: I1, val f: I2) {
                inline fun foo(g: T, h: I1, i: I2) = a.length +
                        b.length + c.length +
                        d.length + e.length + f.length +
                        g.length + h.length + i.length

                fun bar(g: T, h: I1, i: I2) = a.length +
                        b.length + c.length +
                        d.length + e.length + f.length +
                        g.length + h.length + i.length
        }
    }
}

fun box(): String {
    val arguments = listOf<String>("0123456789", "", "\n")
    for (arg in arguments)
        if (A(arg).Inner1("0", "01").Inner2("0", "01", "012").foo("0", "01", "012") !=
            A(arg).Inner1("0", "01").Inner2("0", "01", "012").bar("0", "01", "012"))
            return arg
    return "OK"
}