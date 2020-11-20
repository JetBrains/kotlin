// !LANGUAGE: +InlineClasses
// JAVAC_SKIP

// FILE: uint.kt

package kotlin

inline class UByte(private val b: Byte)
inline class UShort(private val s: Short)
inline class UInt(private val i: Int)
inline class ULong(private val l: Long)

// FILE: test.kt

annotation class AnnoUB(val ub0: <!UNRESOLVED_REFERENCE!>UByte<!>, val ub1: <!UNRESOLVED_REFERENCE!>UByte<!>)
annotation class AnnoUS(val us0: <!UNRESOLVED_REFERENCE!>UShort<!>, val us1: <!UNRESOLVED_REFERENCE!>UShort<!>)
annotation class AnnoUI(val ui0: <!UNRESOLVED_REFERENCE!>UInt<!>, val ui1: <!UNRESOLVED_REFERENCE!>UInt<!>, val ui2: <!UNRESOLVED_REFERENCE!>UInt<!>, val ui3: <!UNRESOLVED_REFERENCE!>UInt<!>)
annotation class AnnoUL(val ul0: <!UNRESOLVED_REFERENCE!>ULong<!>, val ul1: <!UNRESOLVED_REFERENCE!>ULong<!>)

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

const val explicit: <!UNRESOLVED_REFERENCE!>UInt<!> = <!UNRESOLVED_REFERENCE!>UInt<!>(2)

const val nullable: <!UNRESOLVED_REFERENCE!>UInt?<!> = <!UNRESOLVED_REFERENCE!>UInt<!>(3)

annotation class NullableAnno(val u: <!UNRESOLVED_REFERENCE!>UInt?<!>)