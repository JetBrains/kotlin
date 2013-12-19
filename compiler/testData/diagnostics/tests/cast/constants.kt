fun asCall() {
    1 <!USELESS_CAST!>as<!> Int
    1 <!CAST_NEVER_SUCCEEDS!>as<!> Byte
    1 <!CAST_NEVER_SUCCEEDS!>as<!> Short
    1 <!CAST_NEVER_SUCCEEDS!>as<!> Long
    1 <!CAST_NEVER_SUCCEEDS!>as<!> Char
    1 <!CAST_NEVER_SUCCEEDS!>as<!> Double
    1 <!CAST_NEVER_SUCCEEDS!>as<!> Float

    1.0 <!CAST_NEVER_SUCCEEDS!>as<!> Int
    1.0 <!CAST_NEVER_SUCCEEDS!>as<!> Byte
    1.0 <!CAST_NEVER_SUCCEEDS!>as<!> Short
    1.0 <!CAST_NEVER_SUCCEEDS!>as<!> Long
    1.0 <!CAST_NEVER_SUCCEEDS!>as<!> Char
    1.0 <!USELESS_CAST!>as<!> Double
    1.0 <!CAST_NEVER_SUCCEEDS!>as<!> Float

    1f <!CAST_NEVER_SUCCEEDS!>as<!> Int
    1f <!CAST_NEVER_SUCCEEDS!>as<!> Byte
    1f <!CAST_NEVER_SUCCEEDS!>as<!> Short
    1f <!CAST_NEVER_SUCCEEDS!>as<!> Long
    1f <!CAST_NEVER_SUCCEEDS!>as<!> Char
    1f <!CAST_NEVER_SUCCEEDS!>as<!> Double
    1f <!USELESS_CAST!>as<!> Float
}

fun asSafe() {
    1 <!USELESS_CAST!>as?<!> Int
    1 <!CAST_NEVER_SUCCEEDS!>as?<!> Byte
    1 <!CAST_NEVER_SUCCEEDS!>as?<!> Short
    1 <!CAST_NEVER_SUCCEEDS!>as?<!> Long
    1 <!CAST_NEVER_SUCCEEDS!>as?<!> Char
    1 <!CAST_NEVER_SUCCEEDS!>as?<!> Double
    1 <!CAST_NEVER_SUCCEEDS!>as?<!> Float

    1.0 <!CAST_NEVER_SUCCEEDS!>as?<!> Int
    1.0 <!CAST_NEVER_SUCCEEDS!>as?<!> Byte
    1.0 <!CAST_NEVER_SUCCEEDS!>as?<!> Short
    1.0 <!CAST_NEVER_SUCCEEDS!>as?<!> Long
    1.0 <!CAST_NEVER_SUCCEEDS!>as?<!> Char
    1.0 <!USELESS_CAST!>as?<!> Double
    1.0 <!CAST_NEVER_SUCCEEDS!>as?<!> Float

    1f <!CAST_NEVER_SUCCEEDS!>as?<!> Int
    1f <!CAST_NEVER_SUCCEEDS!>as?<!> Byte
    1f <!CAST_NEVER_SUCCEEDS!>as?<!> Short
    1f <!CAST_NEVER_SUCCEEDS!>as?<!> Long
    1f <!CAST_NEVER_SUCCEEDS!>as?<!> Char
    1f <!CAST_NEVER_SUCCEEDS!>as?<!> Double
    1f <!USELESS_CAST!>as?<!> Float
}