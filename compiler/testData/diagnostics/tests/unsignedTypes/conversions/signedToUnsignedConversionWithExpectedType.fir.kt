// DIAGNOSTICS: -UNUSED_PARAMETER

fun takeUByte(u: UByte) {}
fun takeUShort(u: UShort) {}
fun takeUInt(u: UInt) {}
fun takeULong(u: ULong) {}

fun takeUBytes(vararg u: <!OPT_IN_USAGE!>UByte<!>) {}

fun takeNullableUInt(u: UInt?) {}

fun test() {
    takeUInt(<!ARGUMENT_TYPE_MISMATCH!>1 + 2<!>)
    takeUInt(<!ARGUMENT_TYPE_MISMATCH!>1.plus(2)<!>)
    takeNullableUInt(<!ARGUMENT_TYPE_MISMATCH!>4<!>)

    takeUInt(<!ARGUMENT_TYPE_MISMATCH!>Int.MAX_VALUE * 2L<!>)
    takeUInt(<!ARGUMENT_TYPE_MISMATCH!>-1<!>)
    takeUInt(<!ARGUMENT_TYPE_MISMATCH!>Int.MAX_VALUE * 2L + 2<!>)

    takeUByte(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    takeUByte(<!ARGUMENT_TYPE_MISMATCH!>255<!>)
    takeUByte(<!ARGUMENT_TYPE_MISMATCH!>1.toByte()<!>)

    takeUShort(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    takeUInt(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    takeULong(<!ARGUMENT_TYPE_MISMATCH!>1<!>)

    takeULong(<!INT_LITERAL_OUT_OF_RANGE!>18446744073709551615<!>)
    takeULong(<!ARGUMENT_TYPE_MISMATCH!>1844674407370955161<!>)
    takeULong(18446744073709551615u)

    takeUInt(<!ARGUMENT_TYPE_MISMATCH!>Int.MAX_VALUE * 2<!>)
    takeUInt(<!ARGUMENT_TYPE_MISMATCH!>4294967294<!>)

    <!OPT_IN_USAGE!>takeUBytes<!>(<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_TYPE_MISMATCH!>255<!>, <!ARGUMENT_TYPE_MISMATCH!>256<!>, <!ARGUMENT_TYPE_MISMATCH!>0<!>, <!ARGUMENT_TYPE_MISMATCH!>-1<!>, <!ARGUMENT_TYPE_MISMATCH!>40 + 2<!>)

    takeUInt(<!ARGUMENT_TYPE_MISMATCH!>1.myPlus(2)<!>)

    val localVariable = 42
    takeUInt(<!ARGUMENT_TYPE_MISMATCH!>localVariable<!>)

    var localMutableVariable = 42
    takeUInt(<!ARGUMENT_TYPE_MISMATCH!>localMutableVariable<!>)

    val localNegativeVariable = -1
    takeUInt(<!ARGUMENT_TYPE_MISMATCH!>localNegativeVariable<!>)

    takeUInt(<!ARGUMENT_TYPE_MISMATCH!>globalVariable<!>)

    takeUInt(<!ARGUMENT_TYPE_MISMATCH!>constVal<!>)

    takeUInt(<!ARGUMENT_TYPE_MISMATCH!>globalVariableWithGetter<!>)
}

val globalVariable = 10

const val constVal = 10

val globalVariableWithGetter: Int get() = 0

val prop: UByte = <!INITIALIZER_TYPE_MISMATCH!>255<!>

fun Int.myPlus(other: Int): Int = this + other
