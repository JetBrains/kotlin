// !CHECK_TYPE
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTIONS: constant-literals, integer-literals, long-integer-literals
 PARAGRAPH: 1
 SENTENCE: [2] An integer literal with the long literal mark has type kotlin.Long; an integer literal without it has one of the types kotlin.Int/kotlin.Short/kotlin.Byte (the selected type is dependent on the context), if its value is in range of the corresponding type, or type kotlin.Long otherwise.
 NUMBER: 4
 DESCRIPTION: Type checking of binary integer literals.
 */

fun case_1() {
    0b0 checkType { _<Int>() }
    checkSubtype<Int>(0b0)
    checkSubtype<Short>(0b0)
    checkSubtype<Byte>(0b0)
    checkSubtype<Long>(0b0)

    0B1111111 checkType { _<Int>() }
    checkSubtype<Int>(0B1111111)
    checkSubtype<Short>(0B1111111)
    checkSubtype<Byte>(0B1111111)
    checkSubtype<Long>(0B1111111)

    0b10000000 checkType { _<Int>() }
    checkSubtype<Int>(0b10000000)
    checkSubtype<Short>(0b10000000)
    checkSubtype<Long>(0b10000000)

    -0B10000000 checkType { _<Int>() }
    checkSubtype<Int>(-0B10000000)
    checkSubtype<Short>(-0B10000000)
    checkSubtype<Byte>(-0B10000000)
    checkSubtype<Long>(-0B10000000)

    -0b10000001 checkType { _<Int>() }
    checkSubtype<Int>(-0b10000001)
    checkSubtype<Short>(-0b10000001)
    checkSubtype<Long>(-0b10000001)

    0B111111111111111 checkType { _<Int>() }
    checkSubtype<Int>(0B111111111111111)
    checkSubtype<Short>(0B111111111111111)
    checkSubtype<Long>(0B111111111111111)

    0b1000000000000000 checkType { _<Int>() }
    checkSubtype<Int>(0b1000000000000000)
    checkSubtype<Long>(0b1000000000000000)

    -0b1000000000000000 checkType { _<Int>() }
    checkSubtype<Int>(-0b1000000000000000)
    checkSubtype<Short>(-0b1000000000000000)
    checkSubtype<Long>(-0b1000000000000000)

    -0B1000000000000001 checkType { _<Int>() }
    checkSubtype<Int>(-0B1000000000000001)
    checkSubtype<Long>(-0B1000000000000001)

    0b1111111111111111111111111111111 checkType { _<Int>() }
    checkSubtype<Int>(0b1111111111111111111111111111111)
    checkSubtype<Long>(0b1111111111111111111111111111111)

    0B10000000000000000000000000000000 checkType { _<Long>() }
    checkSubtype<Long>(0B10000000000000000000000000000000)

    -0B10000000000000000000000000000000 checkType { _<Int>() }
    checkSubtype<Int>(-0B10000000000000000000000000000000)
    checkSubtype<Long>(-0B10000000000000000000000000000000)

    -0b10000000000000000000000000000001 checkType { _<Long>() }
    checkSubtype<Long>(-0b10000000000000000000000000000001)

    0b111111111111111111111111111111111111111111111111111111111111111 checkType { _<Long>() }
    checkSubtype<Long>(0b111111111111111111111111111111111111111111111111111111111111111)

    -0B111111111111111111111111111111111111111111111111111111111111111 checkType { _<Long>() }
    checkSubtype<Long>(-0B111111111111111111111111111111111111111111111111111111111111111)
}
