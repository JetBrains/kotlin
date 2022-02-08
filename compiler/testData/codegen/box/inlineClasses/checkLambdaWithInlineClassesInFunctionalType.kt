// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class UInt(val value: Int)
OPTIONAL_JVM_INLINE_ANNOTATION
value class ULong(val value: Long)

fun foo(u: UInt, f: (UInt) -> ULong): ULong = f(u)
inline fun inlinedFoo(u: UInt, f: (UInt) -> ULong): ULong = f(u)

fun mapUIntToULong(u: UInt): ULong = ULong(u.value.toLong())

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