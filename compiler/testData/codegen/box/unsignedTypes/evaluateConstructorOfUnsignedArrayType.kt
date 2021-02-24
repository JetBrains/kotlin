// IGNORE_BACKEND_FIR: JVM_IR
// WITH_REFLECT
// TARGET_BACKEND: JVM

annotation class AnnoUB(val ub: UByteArray)
annotation class AnnoUS(val us: UShortArray)
annotation class AnnoUI(val ui: UIntArray)
annotation class AnnoUL(val ul: ULongArray)

@Suppress("INVISIBLE_MEMBER")
const val ub0 = UByte(1)
@Suppress("INVISIBLE_MEMBER")
const val us0 = UShort(2)
@Suppress("INVISIBLE_MEMBER")
const val ul0 = ULong(3)

@Suppress("INVISIBLE_MEMBER")
const val ui0 = UInt(-1)
@Suppress("INVISIBLE_MEMBER")
const val ui1 = UInt(0)
@Suppress("INVISIBLE_MEMBER")
const val ui2 = UInt(40 + 2)

@Suppress("INVISIBLE_MEMBER")
object Foo {
    @AnnoUB([UByte(1), ub0])
    fun f0() {}

    @AnnoUS([UShort(2 + 5), us0])
    fun f1() {}

    @AnnoUI([ui0, ui1, ui2, UInt(100)])
    fun f2() {}

    @AnnoUL([ul0, ULong(5)])
    fun f3() {}
}

fun <T> check(ann: Annotation, f: T.() -> Boolean) {
    val result = (ann as T).f()
    if (!result) throw RuntimeException("fail for $ann")
}

@Suppress("INVISIBLE_MEMBER")
fun box(): String {
    if (ub0.toByte() != 1.toByte()) return "fail"
    if (us0.toShort() != 2.toShort()) return "fail"
    if (ul0.toLong() != 3L) return "fail"
    if ((ui0 + ui1 + ui2).toInt() != 41) return "fail"

    check<AnnoUB>(Foo::f0.annotations.first()) {
        this.ub[0] == UByte(1) && this.ub[1] == UByte(1)
    }

    check<AnnoUS>(Foo::f1.annotations.first()) {
        this.us[0] == UShort(7) && this.us[1] == UShort(2)
    }

    check<AnnoUI>(Foo::f2.annotations.first()) {
        this.ui[0] == UInt.MAX_VALUE && this.ui[1] == UInt(0) && this.ui[2] == UInt(42) && this.ui[3] == UInt(100)
    }

    check<AnnoUL>(Foo::f3.annotations.first()) {
        this.ul[0] == ULong(3) && this.ul[1] == ULong(5)
    }

    return "OK"
}