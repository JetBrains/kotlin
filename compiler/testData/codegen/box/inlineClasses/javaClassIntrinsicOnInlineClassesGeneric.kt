// WITH_STDLIB
// TARGET_BACKEND: JVM
// WORKS_WHEN_VALUE_CLASS
// IGNORE_BACKEND: JVM
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

package root

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcInt<T: Int>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcLong<T: Long>(val l: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcAny<T>(val a: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcOverIc<T: IcLong<Long>>(val o: T)

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

    reifiedCheck<IcInt<Int>>("class root.IcInt")
    reifiedCheck<IcLong<Long>>("class root.IcLong")
    reifiedCheck<IcAny<Any?>>("class root.IcAny")
    reifiedCheck<IcOverIc<IcLong<Long>>>("class root.IcOverIc")
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