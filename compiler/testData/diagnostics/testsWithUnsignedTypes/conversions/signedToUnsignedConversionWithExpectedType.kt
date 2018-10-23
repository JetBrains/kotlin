// !DIAGNOSTICS: -UNUSED_PARAMETER

fun takeUByte(u: UByte) {}
fun takeUShort(u: UShort) {}
fun takeUInt(u: UInt) {}
fun takeULong(u: ULong) {}

fun takeUBytes(vararg u: UByte) {}

fun takeNullableUInt(u: UInt?) {}

fun test() {
    takeUInt(<!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED!>1 + 2<!>)
    takeUInt(<!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED!>1.plus(2)<!>)
    takeNullableUInt(<!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED!>4<!>)

    takeUInt(<!TYPE_MISMATCH!>Int.MAX_VALUE * 2L<!>)
    takeUInt(<!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED, TYPE_MISMATCH!>-1<!>)
    takeUInt(<!TYPE_MISMATCH!>Int.MAX_VALUE * 2L + 2<!>)

    takeUByte(<!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED!>1<!>)
    takeUByte(<!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED!>255<!>)
    takeUByte(<!TYPE_MISMATCH!>1.toByte()<!>)

    takeUShort(<!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED!>1<!>)
    takeUInt(<!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED!>1<!>)
    takeULong(<!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED!>1<!>)

    takeULong(<!INT_LITERAL_OUT_OF_RANGE!>18446744073709551615<!>)
    takeULong(<!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED!>1844674407370955161<!>)
    takeULong(18446744073709551615u)

    takeUInt(<!INTEGER_OVERFLOW, TYPE_MISMATCH!>Int.MAX_VALUE * 2<!>)
    takeUInt(<!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED!>4294967294<!>)

    takeUBytes(<!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED!>1<!>, <!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED!>2<!>, <!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED!>255<!>, <!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED, CONSTANT_EXPECTED_TYPE_MISMATCH!>256<!>, <!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED!>0<!>, <!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED, TYPE_MISMATCH!>-1<!>, <!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED!>40 + 2<!>)

    takeUInt(<!TYPE_MISMATCH!>1.myPlus(2)<!>)

    val localVariable = 42
    takeUInt(<!TYPE_MISMATCH!>localVariable<!>)

    var localMutableVariable = 42
    takeUInt(<!TYPE_MISMATCH!>localMutableVariable<!>)

    val localNegativeVariable = -1
    takeUInt(<!TYPE_MISMATCH!>localNegativeVariable<!>)

    takeUInt(<!TYPE_MISMATCH!>globalVariable<!>)

    takeUInt(<!TYPE_MISMATCH!>constVal<!>)

    takeUInt(<!TYPE_MISMATCH!>globalVariableWithGetter<!>)
}

val globalVariable = 10

const val constVal = 10

val globalVariableWithGetter: Int get() = 0

val prop: UByte = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>255<!>

fun Int.myPlus(other: Int): Int = this + other
