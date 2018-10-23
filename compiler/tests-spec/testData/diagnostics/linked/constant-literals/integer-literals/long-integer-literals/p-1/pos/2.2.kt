// !CHECK_TYPE
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTIONS: constant-literals, integer-literals, long-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] An integer literal with the long literal mark has type kotlin.Long; an integer literal without it has one of the types kotlin.Int/kotlin.Short/kotlin.Byte (the selected type is dependent on the context), if its value is in range of the corresponding type, or type kotlin.Long otherwise.
 NUMBER: 2
 DESCRIPTION: Type checking of decimal integer literals.
 */

fun case_1() {
    0 checkType { _<Int>() }
    checkSubtype<Int>(0)
    checkSubtype<Short>(0)
    checkSubtype<Byte>(0)
    checkSubtype<Long>(0)

    127 checkType { _<Int>() }
    checkSubtype<Int>(127)
    checkSubtype<Short>(127)
    checkSubtype<Byte>(127)
    checkSubtype<Long>(127)

    128 checkType { _<Int>() }
    checkSubtype<Int>(128)
    checkSubtype<Short>(128)
    checkSubtype<Long>(128)

    -128 checkType { _<Int>() }
    checkSubtype<Int>(-128)
    checkSubtype<Short>(-128)
    checkSubtype<Byte>(-128)
    checkSubtype<Long>(-128)

    -129 checkType { _<Int>() }
    checkSubtype<Int>(-129)
    checkSubtype<Short>(-129)
    checkSubtype<Long>(-129)

    32767 checkType { _<Int>() }
    checkSubtype<Int>(32767)
    checkSubtype<Short>(32767)
    checkSubtype<Long>(32767)

    32768 checkType { _<Int>() }
    checkSubtype<Int>(32768)
    checkSubtype<Long>(32768)

    -32768 checkType { _<Int>() }
    checkSubtype<Int>(-32768)
    checkSubtype<Short>(-32768)
    checkSubtype<Long>(-32768)

    -32769 checkType { _<Int>() }
    checkSubtype<Int>(-32769)
    checkSubtype<Long>(-32769)

    2147483647 checkType { _<Int>() }
    checkSubtype<Int>(2147483647)
    checkSubtype<Long>(2147483647)

    2147483648 checkType { _<Long>() }
    checkSubtype<Long>(2147483648)

    -2147483648 checkType { _<Int>() }
    checkSubtype<Int>(-2147483648)
    checkSubtype<Long>(-2147483648)

    -2147483649 checkType { _<Long>() }
    checkSubtype<Long>(-2147483649)

    9223372036854775807 checkType { _<Long>() }
    checkSubtype<Long>(9223372036854775807)

    -9223372036854775807 checkType { _<Long>() }
    checkSubtype<Long>(-9223372036854775807)
}
