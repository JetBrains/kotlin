// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// WITH_REFLECT
// TARGET_BACKEND: JVM

@file:Suppress("INVISIBLE_MEMBER")

annotation class AnnoUB(val ub0: UByte, val ub1: UByte)
annotation class AnnoUS(val us0: UShort, val us1: UShort)
annotation class AnnoUI(val ui0: UInt, val ui1: UInt, val ui2: UInt, val ui3: UInt)
annotation class AnnoUL(val ul0: ULong, val ul1: ULong)

const val ub0 = UByte(1)
const val us0 = UShort(2)
const val ul0 = ULong(3)

const val ui0 = UInt(-1)
const val ui1 = UInt(0)
const val ui2 = UInt(40 + 2)

object Foo {
    @AnnoUB(UByte(1), ub0)
    fun f0() {}

    @AnnoUS(UShort(2 + 5), us0)
    fun f1() {}

    @AnnoUI(ui0, ui1, ui2, UInt(100))
    fun f2() {}

    @AnnoUL(ul0, ULong(5))
    fun f3() {}
}

fun <T> check(ann: Annotation, f: T.() -> Boolean) {
    val result = (ann as T).f()
    if (!result) throw RuntimeException("fail for $ann")
}

fun box(): String {
    if (ub0.toByte() != 1.toByte()) return "fail"
    if (us0.toShort() != 2.toShort()) return "fail"
    if (ul0.toLong() != 3L) return "fail"
    if ((ui0 + ui1 + ui2).toInt() != 41) return "fail"

    check<AnnoUB>(Foo::f0.annotations.first()) {
        this.ub0 == UByte(1) && this.ub1 == UByte(1)
    }

    check<AnnoUS>(Foo::f1.annotations.first()) {
        this.us0 == UShort(7) && this.us1 == UShort(2)
    }

    check<AnnoUI>(Foo::f2.annotations.first()) {
        this.ui0 == UInt.MAX_VALUE && this.ui1 == UInt(0) && this.ui2 == UInt(42) && this.ui3 == UInt(100)
    }

    check<AnnoUL>(Foo::f3.annotations.first()) {
        this.ul0 == ULong(3) && this.ul1 == ULong(5)
    }

    return "OK"
}