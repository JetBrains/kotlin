// WITH_STDLIB
// TARGET_BACKEND: JVM

package root

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class IcInt(val x: Int)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class IcLong(val l: Long)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class IcAny(val a: Any?)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class IcOverIc(val o: IcLong)

fun check(c: Class<*>, s: String) {
    if (c.toString() != s) error("Fail, expected: $s, actual: $c")
}

inline fun <reified T> reifiedCheck(asString: String) {
    check(T::class.java, asString)
}

fun box(): String {
    val i = IcInt(0)
    val l = IcLong(0)
    val a = IcAny("foo")
    val o = IcOverIc(IcLong(0))

    check(i.javaClass, "class root.IcInt")
    check(l.javaClass, "class root.IcLong")
    check(a.javaClass, "class root.IcAny")
    check(o.javaClass, "class root.IcOverIc")
    check(1u.javaClass, "class kotlin.UInt")

    check(i::class.java, "class root.IcInt")
    check(l::class.java, "class root.IcLong")
    check(a::class.java, "class root.IcAny")
    check(o::class.java, "class root.IcOverIc")
    check(1u::class.java, "class kotlin.UInt")

    reifiedCheck<IcInt>("class root.IcInt")
    reifiedCheck<IcLong>("class root.IcLong")
    reifiedCheck<IcAny>("class root.IcAny")
    reifiedCheck<IcOverIc>("class root.IcOverIc")
    reifiedCheck<UInt>("class kotlin.UInt")

    val arrI = arrayOf(i)
    check(arrI[0].javaClass, "class root.IcInt")

    val arrL = arrayOf(l)
    check(arrL[0].javaClass, "class root.IcLong")

    val arrA = arrayOf(a)
    check(arrA[0].javaClass, "class root.IcAny")

    val arrO = arrayOf(o)
    check(arrO[0].javaClass, "class root.IcOverIc")

    val arrU = arrayOf(1u)
    check(arrU[0].javaClass, "class kotlin.UInt")

    return "OK"
}