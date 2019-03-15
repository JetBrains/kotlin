// !LANGUAGE: +InlineClasses
// JAVAC_SKIP

// FILE: uint.kt

package kotlin

inline class UByte(private val b: Byte)
inline class UShort(private val s: Short)
inline class UInt(private val i: Int)
inline class ULong(private val l: Long)

// FILE: test.kt

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

@AnnoUB(UByte(1), ub0)
fun f0() {}

@AnnoUS(UShort(2 + 5), us0)
fun f1() {}

@AnnoUI(ui0, ui1, ui2, UInt(100))
fun f2() {}

@AnnoUL(ul0, ULong(5))
fun f3() {}

const val explicit: UInt = UInt(2)

<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val nullable: UInt? = UInt(3)

annotation class NullableAnno(val u: <!NULLABLE_TYPE_OF_ANNOTATION_MEMBER!>UInt?<!>)