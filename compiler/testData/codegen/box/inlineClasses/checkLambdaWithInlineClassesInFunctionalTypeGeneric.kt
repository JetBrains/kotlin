// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class UInt<T: Int>(val value: T)
OPTIONAL_JVM_INLINE_ANNOTATION
value class ULong<T: Long>(val value: T)

fun foo(u: UInt<Int>, f: (UInt<Int>) -> ULong<Long>): ULong<Long> = f(u)
inline fun inlinedFoo(u: UInt<Int>, f: (UInt<Int>) -> ULong<Long>): ULong<Long> = f(u)

fun mapUIntToULong(u: UInt<Int>): ULong<Long> = ULong(u.value.toLong())

fun box(): String {
    val u = UInt(123)
    val l1 = foo(u) {
        mapUIntToULong(it)
    }

    if (l1.value != 123L) return "fail"

    val l2 = inlinedFoo(UInt(10)) {
        mapUIntToULong(it)
    }

    if (l2.value != 10L) return "fail"

    return "OK"
}