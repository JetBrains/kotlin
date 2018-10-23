// !CHECK_TYPE
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTIONS: constant-literals, integer-literals, long-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] An integer literal with the long literal mark has type kotlin.Long; an integer literal without it has one of the types kotlin.Int/kotlin.Short/kotlin.Byte (the selected type is dependent on the context), if its value is in range of the corresponding type, or type kotlin.Long otherwise.
 NUMBER: 3
 DESCRIPTION: Type checking of hexadecimal integer literals.
 */

fun case_1() {
    0x0 checkType { _<Int>() }
    checkSubtype<Int>(0x0)
    checkSubtype<Short>(0x0)
    checkSubtype<Byte>(0x0)
    checkSubtype<Long>(0x0)

    0x7F checkType { _<Int>() }
    checkSubtype<Int>(0x7F)
    checkSubtype<Short>(0x7F)
    checkSubtype<Byte>(0x7F)
    checkSubtype<Long>(0x7F)

    0X80 checkType { _<Int>() }
    checkSubtype<Int>(0X80)
    checkSubtype<Short>(0X80)
    checkSubtype<Long>(0X80)

    -0X80 checkType { _<Int>() }
    checkSubtype<Int>(-0X80)
    checkSubtype<Short>(-0X80)
    checkSubtype<Byte>(-0X80)
    checkSubtype<Long>(-0X80)

    -0x81 checkType { _<Int>() }
    checkSubtype<Int>(-0x81)
    checkSubtype<Short>(-0x81)
    checkSubtype<Long>(-0x81)

    0x7FFF checkType { _<Int>() }
    checkSubtype<Int>(0x7FFF)
    checkSubtype<Short>(0x7FFF)
    checkSubtype<Long>(0x7FFF)

    0x8000 checkType { _<Int>() }
    checkSubtype<Int>(0x8000)
    checkSubtype<Long>(0x8000)

    -0x8000 checkType { _<Int>() }
    checkSubtype<Int>(-0x8000)
    checkSubtype<Short>(-0x8000)
    checkSubtype<Long>(-0x8000)

    -0X8001 checkType { _<Int>() }
    checkSubtype<Int>(-0X8001)
    checkSubtype<Long>(-0X8001)

    0x7FFFFFFF checkType { _<Int>() }
    checkSubtype<Int>(0x7FFFFFFF)
    checkSubtype<Long>(0x7FFFFFFF)

    0x80000000 checkType { _<Long>() }
    checkSubtype<Long>(0x80000000)

    -0x80000000 checkType { _<Int>() }
    checkSubtype<Int>(-0x80000000)
    checkSubtype<Long>(-0x80000000)

    -0x80000001 checkType { _<Long>() }
    checkSubtype<Long>(-0x80000001)

    0X7FFFFFFFFFFFFFFF checkType { _<Long>() }
    checkSubtype<Long>(0X7FFFFFFFFFFFFFFF)

    -0X7FFFFFFFFFFFFFFF checkType { _<Long>() }
    checkSubtype<Long>(-0X7FFFFFFFFFFFFFFF)
}
