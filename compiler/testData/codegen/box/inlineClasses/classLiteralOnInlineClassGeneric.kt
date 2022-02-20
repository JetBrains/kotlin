// WITH_STDLIB
// WITH_REFLECT
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

package root

import kotlin.reflect.KClass

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcInt<T: Int>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcLong<T: Long>(val l: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcAny<T>(val a: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcOverIc<T: IcLong<Long>>(val o: T)

fun check(c: KClass<*>, s: String) {
    if (c.toString() != s) error("Fail, expected: $s, actual: $c")
}

fun check(actual: String?, expected: String) {
    if (actual != expected) error("Fail, expected: $expected, actual: $actual")
}

inline fun <reified T> reifiedCheck(asString: String, simpleName: String) {
    check(T::class, asString)
    check(T::class.simpleName, simpleName)
}

fun box(): String {
    val i = IcInt(0)
    val l = IcLong(0)
    val a = IcAny("foo")
    val o = IcOverIc(IcLong(0))

    check(i::class, "class root.IcInt")
    check(l::class, "class root.IcLong")
    check(a::class, "class root.IcAny")
    check(o::class, "class root.IcOverIc")
    check(1u::class, "class kotlin.UInt")

    check(i::class.simpleName, "IcInt")
    check(l::class.simpleName, "IcLong")
    check(a::class.simpleName, "IcAny")
    check(o::class.simpleName, "IcOverIc")
    check(1u::class.simpleName, "UInt")

    reifiedCheck<IcInt<Int>>("class root.IcInt", "IcInt")
    reifiedCheck<IcLong<Long>>("class root.IcLong", "IcLong")
    reifiedCheck<IcAny<Any?>>("class root.IcAny", "IcAny")
    reifiedCheck<IcOverIc<IcLong<Long>>>("class root.IcOverIc", "IcOverIc")
    reifiedCheck<UInt>("class kotlin.UInt", "UInt")

    val arrI = arrayOf(i)
    check(arrI[0]::class, "class root.IcInt")

    val arrL = arrayOf(l)
    check(arrL[0]::class, "class root.IcLong")

    val arrA = arrayOf(a)
    check(arrA[0]::class, "class root.IcAny")

    val arrO = arrayOf(o)
    check(arrO[0]::class, "class root.IcOverIc")

    val arrU = arrayOf(1u)
    check(arrU[0]::class, "class kotlin.UInt")

    check(IcInt::class, "class root.IcInt")
    check(IcLong::class, "class root.IcLong")
    check(IcAny::class, "class root.IcAny")
    check(IcOverIc::class, "class root.IcOverIc")
    check(UInt::class, "class kotlin.UInt")

    return "OK"
}