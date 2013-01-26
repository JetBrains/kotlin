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

    <!TYPE_MISMATCH!>1<!>: Double
    <!TYPE_MISMATCH!>1<!>: Float
    
    1 <!USELESS_CAST!>as<!> Byte
    1 <!USELESS_CAST!>as<!> Int
    0xff <!USELESS_CAST!>as<!> Long
    
    <!ERROR_COMPILE_TIME_VALUE!>1.1<!> <!CAST_NEVER_SUCCEEDS!>as<!> Int
    <!TYPE_MISMATCH!>1.1<!>: Int
}