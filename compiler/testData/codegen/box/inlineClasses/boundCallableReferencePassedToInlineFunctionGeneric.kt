// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcInt<T: Int>(val i: T) {
    fun simple(): String = i.toString()
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcLong<T: Long>(val l: T) {
    fun simple(): String = l.toString()
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcAny<T>(val a: T) {
    fun simple(): String = a?.toString() ?: "null"
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcOverIc<T: IcLong<Long>>(val o: T) {
    fun simple(): String = o.toString()
}

fun testUnboxed(i: IcInt<Int>, l: IcLong<Long>, a: IcAny<Int>, o: IcOverIc<IcLong<Long>>): String =
    foo(i::simple) + foo(l::simple) + foo(a::simple) + foo(o::simple)

fun testBoxed(i: IcInt<Int>?, l: IcLong<Long>?, a: IcAny<Int>?, o: IcOverIc<IcLong<Long>>?): String =
    foo(i!!::simple) + foo(l!!::simple) + foo(a!!::simple) + foo(o!!::simple)

fun testLocalVars(): String {
    val i = IcInt(0)
    val l = IcLong(1L)
    val a = IcAny(2)
    val o = IcOverIc(IcLong(3))

    return foo(i::simple) + foo(l::simple) + foo(a::simple) + foo(o::simple)
}

val ip = IcInt(1)
val lp = IcLong(2L)
val ap = IcAny(3)
val op = IcOverIc(IcLong(4))

fun testGlobalProperties(): String =
    foo(ip::simple) + foo(lp::simple) + foo(ap::simple) + foo(op::simple)

fun testCapturedVars(): String {
    return IcInt(2).let { foo(it::simple) } +
            IcLong(3).let { foo(it::simple) } +
            IcAny(4).let { foo(it::simple) } +
            IcOverIc(IcLong(5)).let { foo(it::simple) }
}

inline fun foo(init: () -> String): String = init()

fun box(): String {
    val i = IcInt(3)
    val l = IcLong(4)
    val a = IcAny(5)
    val o = IcOverIc(IcLong(6))

    if (testUnboxed(i, l, a, o) != "345IcLong(l=6)") return "Fail 1 ${testUnboxed(i, l, a, o)}"
    if (testBoxed(i, l, a, o) != "345IcLong(l=6)") return "Fail 2"
    if (testLocalVars() != "012IcLong(l=3)") return "Fail 3"
    if (testGlobalProperties() != "123IcLong(l=4)") return "Fail 4"
    if (testCapturedVars() != "234IcLong(l=5)") return "Fail 5 ${testCapturedVars()}"

    return "OK"
}
