// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

fun takeUByte(u: UByte) {}
fun takeUShort(u: UShort) {}
fun takeUInt(u: UInt) {}
fun takeULong(u: ULong) {}

fun takeUBytes(vararg u: UByte) {}

fun takeNullableUInt(u: UInt?) {}

fun test() {
    <!INAPPLICABLE_CANDIDATE!>takeUInt<!>(1 + 2)
    <!INAPPLICABLE_CANDIDATE!>takeUInt<!>(1.plus(2))
    <!INAPPLICABLE_CANDIDATE!>takeNullableUInt<!>(4)

    <!INAPPLICABLE_CANDIDATE!>takeUInt<!>(Int.MAX_VALUE * 2L)
    <!INAPPLICABLE_CANDIDATE!>takeUInt<!>(-1)
    <!INAPPLICABLE_CANDIDATE!>takeUInt<!>(Int.MAX_VALUE * 2L + 2)

    <!INAPPLICABLE_CANDIDATE!>takeUByte<!>(1)
    <!INAPPLICABLE_CANDIDATE!>takeUByte<!>(255)
    <!INAPPLICABLE_CANDIDATE!>takeUByte<!>(1.toByte())

    <!INAPPLICABLE_CANDIDATE!>takeUShort<!>(1)
    <!INAPPLICABLE_CANDIDATE!>takeUInt<!>(1)
    <!INAPPLICABLE_CANDIDATE!>takeULong<!>(1)

    takeULong(<!ILLEGAL_CONST_EXPRESSION!>18446744073709551615<!>)
    <!INAPPLICABLE_CANDIDATE!>takeULong<!>(1844674407370955161)
    takeULong(18446744073709551615u)

    <!INAPPLICABLE_CANDIDATE!>takeUInt<!>(Int.MAX_VALUE * 2)
    <!INAPPLICABLE_CANDIDATE!>takeUInt<!>(4294967294)

    <!INAPPLICABLE_CANDIDATE!>takeUBytes<!>(1, 2, 255, 256, 0, -1, 40 + 2)

    <!INAPPLICABLE_CANDIDATE!>takeUInt<!>(1.myPlus(2))

    val localVariable = 42
    <!INAPPLICABLE_CANDIDATE!>takeUInt<!>(localVariable)

    var localMutableVariable = 42
    <!INAPPLICABLE_CANDIDATE!>takeUInt<!>(localMutableVariable)

    val localNegativeVariable = -1
    <!INAPPLICABLE_CANDIDATE!>takeUInt<!>(localNegativeVariable)

    <!INAPPLICABLE_CANDIDATE!>takeUInt<!>(globalVariable)

    <!INAPPLICABLE_CANDIDATE!>takeUInt<!>(constVal)

    <!INAPPLICABLE_CANDIDATE!>takeUInt<!>(globalVariableWithGetter)
}

val globalVariable = 10

const val constVal = 10

val globalVariableWithGetter: Int get() = 0

val prop: UByte = <!INITIALIZER_TYPE_MISMATCH!>255<!>

fun Int.myPlus(other: Int): Int = this + other
