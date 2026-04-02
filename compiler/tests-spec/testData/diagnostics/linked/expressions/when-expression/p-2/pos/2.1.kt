// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: expressions, when-expression -> paragraph 2 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: When without bound value, various boolean values in the when condition.
 * HELPERS: typesProvider, enumClasses, sealedClasses, classes
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Boolean, value_2: Long): Int {
    return when {
        value_1 -> 1
        getBoolean() && value_1 -> 2
        getChar() != 'a' -> 3
        Out<Int>() === getAny() -> 4
        value_2 <= 11 -> 5
        !value_1 -> 6
        else -> 7
    }
}

/*
 * TESTCASE NUMBER: 2
 * NOTE: for a potential analysys of exhaustiveness by enums in whens without a bound value.
 */
fun case_2(value_1: EnumClass) {
    when {
        value_1 == EnumClass.NORTH -> {}
        value_1 == EnumClass.SOUTH -> {}
        value_1 == EnumClass.WEST -> {}
        value_1 == EnumClass.EAST -> {}
    }
}

/*
 * TESTCASE NUMBER: 3
 * NOTE: for a potential analysys of exhaustiveness by enums in whens without a bound value.
 */
fun case_3(value_1: Boolean) {
    when {
        value_1 == true -> return
        value_1 == false -> return
    }
}

/*
 * TESTCASE NUMBER: 4
 * NOTE: for a potential mark the code after the true branch as unreacable.
 */
fun case_4(value_1: Boolean) {
    when {
        false -> return
        true -> return
        value_1 -> return
    }
}

/*
 * TESTCASE NUMBER: 5
 * NOTE: for a potential const propagation.
 */
fun case_5(value_1: Boolean) {
    val value_2 = false
    val value_3 = false || !!!false || false

    when {
        value_3 -> return
        value_2 -> return
        value_1 -> return
    }
}

// TESTCASE NUMBER: 6
fun case_6(value_1: Any) {
    when {
        value_1 is Nothing -> {}
        value_1 is Int -> {}
        value_1 is Boolean -> {}
        value_1 is String -> {}
        value_1 is Number -> {}
        value_1 is Float -> {}
        <!USELESS_IS_CHECK!>value_1 is Any<!> -> {}
    }
}

/*
 * TESTCASE NUMBER: 7
 * NOTE: for a potential analysys of exhaustiveness by enums in whens without a bound value.
 */
fun case_7(value_1: Any) {
    when {
        value_1 !is Number -> {}
        value_1 is Float -> {}
        <!USELESS_IS_CHECK!>value_1 is Number<!> -> {}
        <!USELESS_IS_CHECK!>value_1 is Any<!> -> {}
    }
}

/*
 * TESTCASE NUMBER: 8
 * NOTE: for a potential analysys of exhaustiveness by enums in whens without a bound value.
 */
fun case_8(value_1: SealedClass) {
    when {
        value_1 is SealedChild1 -> {}
        value_1 is SealedChild2 -> {}
        value_1 is SealedChild3 -> {}
    }
}

// TESTCASE NUMBER: 9
fun case_9(value_1: Int, value_2: IntRange) {
    when {
        value_1 in -10..100L -> {}
        value_1 in value_2 -> {}
        value_1 !in listOf(0, 1, 2) -> {}
    }
}
