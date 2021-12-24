// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

value class F1(val x: Int)

value class F2(val x: UInt)

value class F3(val x: String)

OPTIONAL_JVM_INLINE_ANNOTATION
value class F4(val x: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class F5(val x: UInt)

OPTIONAL_JVM_INLINE_ANNOTATION
value class F6(val x: String)

value class A(
    val f1: F1,
    val f2: F2,
    val f3: F3,
    val f4: F4,
    val f5: F5,
    val f6: F6,
    val f7: Int,
    val f8: UInt,
    val f9: String,
)

value class B(val a1: A, val a2: A) {
    override fun toString(): String {
        return "OverridenBToString(a1 = $a1, a2 = $a2)"
    }
}

fun box(): String {
    val f1 = F1(1)
    val f2 = F2(UInt.MAX_VALUE)
    val f3 = F3("234")
    val f4 = F4(5)
    val f5 = F5(UInt.MAX_VALUE.dec())
    val f6 = F6("678")
    val a1 = A(f1, f2, f3, f4, f5, f6, 9, UInt.MAX_VALUE - 2U, "0")
    val a2 = a1
    val b = B(a1, a2)

    assert(f1.x == 1)
    assert(f2.x == UInt.MAX_VALUE)
    assert(f3.x == "234")
    assert(f4.x == 5)
    assert(f5.x == UInt.MAX_VALUE - 1U)
    assert(f6.x == "678")

    assert(f1 == a1.f1)
    assert(f1.hashCode() == a1.f1.hashCode())
    assert(f1.toString() == a1.f1.toString())
    assert(f1 == a2.f1)
    assert(f1.hashCode() == a2.f1.hashCode())
    assert(f1.toString() == a2.f1.toString())
    assert(a1.f1 == a2.f1)
    assert(a1.f1.hashCode() == a2.f1.hashCode())
    assert(a1.f1.toString() == a2.f1.toString())

    assert(f2 == a1.f2)
    assert(f2.hashCode() == a1.f2.hashCode())
    assert(f2.toString() == a1.f2.toString())
    assert(f2 == a2.f2)
    assert(f2.hashCode() == a2.f2.hashCode())
    assert(f2.toString() == a2.f2.toString())
    assert(a1.f2 == a2.f2)
    assert(a1.f2.hashCode() == a2.f2.hashCode())
    assert(a1.f2.toString() == a2.f2.toString())

    assert(f3 == a1.f3)
    assert(f3.hashCode() == a1.f3.hashCode())
    assert(f3.toString() == a1.f3.toString())
    assert(f3 == a2.f3)
    assert(f3.hashCode() == a2.f3.hashCode())
    assert(f3.toString() == a2.f3.toString())
    assert(a1.f3 == a2.f3)
    assert(a1.f3.hashCode() == a2.f3.hashCode())
    assert(a1.f3.toString() == a2.f3.toString())

    assert(f4 == a1.f4)
    assert(f4.hashCode() == a1.f4.hashCode())
    assert(f4.toString() == a1.f4.toString())
    assert(f4 == a2.f4)
    assert(f4.hashCode() == a2.f4.hashCode())
    assert(f4.toString() == a2.f4.toString())
    assert(a1.f4 == a2.f4)
    assert(a1.f4.hashCode() == a2.f4.hashCode())
    assert(a1.f4.toString() == a2.f4.toString())

    assert(f5 == a1.f5)
    assert(f5.hashCode() == a1.f5.hashCode())
    assert(f5.toString() == a1.f5.toString())
    assert(f5 == a2.f5)
    assert(f5.hashCode() == a2.f5.hashCode())
    assert(f5.toString() == a2.f5.toString())
    assert(a1.f5 == a2.f5)
    assert(a1.f5.hashCode() == a2.f5.hashCode())
    assert(a1.f5.toString() == a2.f5.toString())

    assert(f6 == a1.f6)
    assert(f6.hashCode() == a1.f6.hashCode())
    assert(f6.toString() == a1.f6.toString())
    assert(f6 == a2.f6)
    assert(f6.hashCode() == a2.f6.hashCode())
    assert(f6.toString() == a2.f6.toString())
    assert(a1.f6 == a2.f6)
    assert(a1.f6.hashCode() == a2.f6.hashCode())
    assert(a1.f6.toString() == a2.f6.toString())

    assert(9 == a1.f7)
    assert(9.hashCode() == a1.f7.hashCode())
    assert(9.toString() == a1.f7.toString())
    assert(9 == a2.f7)
    assert(9.hashCode() == a2.f7.hashCode())
    assert(9.toString() == a2.f7.toString())
    assert(a1.f7 == a2.f7)
    assert(a1.f7.hashCode() == a2.f7.hashCode())
    assert(a1.f7.toString() == a2.f7.toString())

    assert((UInt.MAX_VALUE - 2U) == a1.f8)
    assert((UInt.MAX_VALUE - 2U).hashCode() == a1.f8.hashCode())
    assert((UInt.MAX_VALUE - 2U).toString() == a1.f8.toString())
    assert((UInt.MAX_VALUE - 2U) == a2.f8)
    assert((UInt.MAX_VALUE - 2U).hashCode() == a2.f8.hashCode())
    assert((UInt.MAX_VALUE - 2U).toString() == a2.f8.toString())
    assert(a1.f8 == a2.f8)
    assert(a1.f8.hashCode() == a2.f8.hashCode())
    assert(a1.f8.toString() == a2.f8.toString())

    assert("0" == a1.f9)
    assert("0".hashCode() == a1.f9.hashCode())
    assert("0".toString() == a1.f9.toString())
    assert("0" == a2.f9)
    assert("0".hashCode() == a2.f9.hashCode())
    assert("0".toString() == a2.f9.toString())
    assert(a1.f9 == a2.f9)
    assert(a1.f9.hashCode() == a2.f9.hashCode())
    assert(a1.f9.toString() == a2.f9.toString())


    assert(a1 == a2)
    assert(a1.hashCode() == a2.hashCode())
    assert(a1.toString() == a2.toString())

    assert(b == b)
    assert(b.toString() == b.toString())
    assert(b.hashCode() == b.hashCode())

    assert(f1.toString() == "to be replaced")
    assert(f2.toString() == "to be replaced")
    assert(f3.toString() == "to be replaced")
    assert(f4.toString() == "to be replaced")
    assert(f5.toString() == "to be replaced")
    assert(f6.toString() == "to be replaced")
    assert(a1.toString() == "to be replaced")
    assert(a2.toString() == "to be replaced")
    assert(b.toString() == "to be replaced")

    return "OK"
}
