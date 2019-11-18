// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

inline class IcInt(val i: Int) {
    fun simple(): String = i.toString()
}

inline class IcLong(val l: Long) {
    fun simple(): String = l.toString()
}

inline class IcAny(val a: Any?) {
    fun simple(): String = a?.toString() ?: "null"
}

inline class IcOverIc(val o: IcLong) {
    fun simple(): String = o.toString()
}

fun testUnboxed(i: IcInt, l: IcLong, a: IcAny, o: IcOverIc): String =
    foo(i::simple) + foo(l::simple) + foo(a::simple) + foo(o::simple)

fun testBoxed(i: IcInt?, l: IcLong?, a: IcAny?, o: IcOverIc?): String =
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

    if (testUnboxed(i, l, a, o) != "345IcLong(l=6)") return "Fail 1"
    if (testBoxed(i, l, a, o) != "345IcLong(l=6)") return "Fail 2"
    if (testLocalVars() != "012IcLong(l=3)") return "Fail 3"
    if (testGlobalProperties() != "123IcLong(l=4)") return "Fail 4"
    if (testCapturedVars() != "234IcLong(l=5)") return "Fail 5"

    return "OK"
}
