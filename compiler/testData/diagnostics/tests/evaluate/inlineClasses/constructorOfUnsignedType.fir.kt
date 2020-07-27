// !LANGUAGE: +InlineClasses
// JAVAC_SKIP

// FILE: uint.kt

package kotlin

inline class UByte(private val b: Byte)
inline class UShort(private val s: Short)
inline class UInt(private val i: Int)
inline class ULong(private val l: Long)

// FILE: test.kt

annotation class AnnoUB(val ub0: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>UByte<!>, val ub1: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>UByte<!>)
annotation class AnnoUS(val us0: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>UShort<!>, val us1: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>UShort<!>)
annotation class AnnoUI(val ui0: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>UInt<!>, val ui1: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>UInt<!>, val ui2: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>UInt<!>, val ui3: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>UInt<!>)
annotation class AnnoUL(val ul0: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>ULong<!>, val ul1: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>ULong<!>)

const val ub0 = <!UNRESOLVED_REFERENCE!>UByte<!>(1)
const val us0 = <!UNRESOLVED_REFERENCE!>UShort<!>(2)
const val ul0 = <!UNRESOLVED_REFERENCE!>ULong<!>(3)

const val ui0 = <!UNRESOLVED_REFERENCE!>UInt<!>(-1)
const val ui1 = <!UNRESOLVED_REFERENCE!>UInt<!>(0)
const val ui2 = <!UNRESOLVED_REFERENCE!>UInt<!>(40 + 2)

@AnnoUB(<!UNRESOLVED_REFERENCE!>UByte<!>(1), ub0)
fun f0() {}

@AnnoUS(<!UNRESOLVED_REFERENCE!>UShort<!>(2 + 5), us0)
fun f1() {}

@AnnoUI(ui0, ui1, ui2, <!UNRESOLVED_REFERENCE!>UInt<!>(100))
fun f2() {}

@AnnoUL(ul0, <!UNRESOLVED_REFERENCE!>ULong<!>(5))
fun f3() {}

const val explicit: <!OTHER_ERROR!>UInt<!> = <!UNRESOLVED_REFERENCE!>UInt<!>(2)

const val nullable: <!OTHER_ERROR!>UInt?<!> = <!UNRESOLVED_REFERENCE!>UInt<!>(3)

annotation class NullableAnno(val u: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>UInt?<!>)