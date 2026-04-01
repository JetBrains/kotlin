// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// WITH_STDLIB
fun asCall() {
    1 <!USELESS_CAST!>as Int<!>
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
    1.0 <!USELESS_CAST!>as Double<!>
    1.0 <!CAST_NEVER_SUCCEEDS!>as<!> Float

    1f <!CAST_NEVER_SUCCEEDS!>as<!> Int
    1f <!CAST_NEVER_SUCCEEDS!>as<!> Byte
    1f <!CAST_NEVER_SUCCEEDS!>as<!> Short
    1f <!CAST_NEVER_SUCCEEDS!>as<!> Long
    1f <!CAST_NEVER_SUCCEEDS!>as<!> Char
    1f <!CAST_NEVER_SUCCEEDS!>as<!> Double
    1f <!USELESS_CAST!>as Float<!>
}

fun asSafe() {
    1 <!USELESS_CAST!>as? Int<!>
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
    1.0 <!USELESS_CAST!>as? Double<!>
    1.0 <!CAST_NEVER_SUCCEEDS!>as?<!> Float

    1f <!CAST_NEVER_SUCCEEDS!>as?<!> Int
    1f <!CAST_NEVER_SUCCEEDS!>as?<!> Byte
    1f <!CAST_NEVER_SUCCEEDS!>as?<!> Short
    1f <!CAST_NEVER_SUCCEEDS!>as?<!> Long
    1f <!CAST_NEVER_SUCCEEDS!>as?<!> Char
    1f <!CAST_NEVER_SUCCEEDS!>as?<!> Double
    1f <!USELESS_CAST!>as? Float<!>
}

fun long() {
    1L <!CAST_NEVER_SUCCEEDS!>as<!> Int
    1L <!CAST_NEVER_SUCCEEDS!>as<!> UInt
    1L <!CAST_NEVER_SUCCEEDS!>as<!> ULong
    1L <!CAST_NEVER_SUCCEEDS!>as<!> Byte
    1L <!CAST_NEVER_SUCCEEDS!>as<!> Short
    1L <!USELESS_CAST!>as Long<!>
    1L <!CAST_NEVER_SUCCEEDS!>as<!> Char
    1L <!CAST_NEVER_SUCCEEDS!>as<!> Double
    1L <!CAST_NEVER_SUCCEEDS!>as<!> Float
}

fun unsigned() {
    1U <!CAST_NEVER_SUCCEEDS!>as<!> Int
    1U <!USELESS_CAST!>as UInt<!>
    1U <!CAST_NEVER_SUCCEEDS!>as<!> ULong
    1U <!CAST_NEVER_SUCCEEDS!>as<!> Byte
    1U <!CAST_NEVER_SUCCEEDS!>as<!> Short
    1U <!CAST_NEVER_SUCCEEDS!>as<!> Long
    1U <!CAST_NEVER_SUCCEEDS!>as<!> Char
    1U <!CAST_NEVER_SUCCEEDS!>as<!> Double
    1U <!CAST_NEVER_SUCCEEDS!>as<!> Float

    1.0 <!CAST_NEVER_SUCCEEDS!>as<!> UInt
    1.0 <!CAST_NEVER_SUCCEEDS!>as<!> UByte
    1.0 <!CAST_NEVER_SUCCEEDS!>as<!> UShort
    1.0 <!CAST_NEVER_SUCCEEDS!>as<!> ULong
    1f <!CAST_NEVER_SUCCEEDS!>as<!> UInt
    1f <!CAST_NEVER_SUCCEEDS!>as<!> UByte
    1f <!CAST_NEVER_SUCCEEDS!>as<!> UShort
    1f <!CAST_NEVER_SUCCEEDS!>as<!> ULong

    1UL <!CAST_NEVER_SUCCEEDS!>as<!> UInt
    1UL <!USELESS_CAST!>as ULong<!>
    1UL <!CAST_NEVER_SUCCEEDS!>as<!> Int
    1UL <!CAST_NEVER_SUCCEEDS!>as<!> Long
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, integerLiteral */
