fun varargByte(vararg v: Byte) = v

fun varargShort(vararg v: Short) = v

fun varargInt(vararg v: Int) = v

fun varargLong(vararg v: Long) = v

fun varargFloat(vararg v: Float) = v

fun varargDouble(vararg v: Double) = v

fun test() {
    1: Byte
    1: Short
    1: Int
    1: Long

    0x001: Long
    0b001: Int

    0.1: Double
    0.1: Float

    1e5: Double
    1e-5: Float

    <!ERROR_COMPILE_TIME_VALUE!>1<!>: Double
    <!ERROR_COMPILE_TIME_VALUE!>1<!>: Float
    
    1 <!USELESS_CAST!>as<!> Byte
    1 <!USELESS_CAST!>as<!> Int
    0xff <!USELESS_CAST!>as<!> Long
    
    <!ERROR_COMPILE_TIME_VALUE!>1.1<!> <!CAST_NEVER_SUCCEEDS!>as<!> Int
    <!ERROR_COMPILE_TIME_VALUE!>1.1<!>: Int

    varargByte(0x77, 1, 3, <!ERROR_COMPILE_TIME_VALUE!>200<!>, 0b111)
    varargShort(0x777, 1, 2, 3, <!ERROR_COMPILE_TIME_VALUE!>200000<!>, 0b111)
    varargInt(0x77777777, <!ERROR_COMPILE_TIME_VALUE!>0x7777777777<!>, 1, 2, 3, 2000000000, 0b111)
    varargLong(0x777777777777, 1, 2, 3, 200000, 0b111)
    varargFloat(<!ERROR_COMPILE_TIME_VALUE!>1<!>, 1.0, <!TYPE_MISMATCH!>-0.1<!>, 1e4, 1e-4, <!TYPE_MISMATCH!>-1e4<!>)
    varargDouble(<!ERROR_COMPILE_TIME_VALUE!>1<!>, 1.0, -0.1, 1e4, 1e-4, -1e4)
}