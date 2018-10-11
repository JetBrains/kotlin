// !WITH_BASIC_TYPES
// !WITH_SEALED_CLASSES
// !WITH_ENUM_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTIONS: when-expression
 PARAGRAPH: 3
 SENTENCE: [2] Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
 NUMBER: 1
 DESCRIPTION: 'When' without bound value and different variants of the boolean conditions (logical, equality, comparison, type checking operator, containment operator).
 */

// CASE DESCRIPTION: 'When' with boolean expressions and else branch.
fun case_1(value_1: Boolean, value_2: Long): Int {
    return when {
        value_1 -> 1
        getBoolean() && value_1 -> 2
        getChar(10) != 'a' -> 3
        getList() === getAny() -> 4
        value_2 <= 11 -> 5
        !value_1 -> 6
        else -> 7
    }
}

/*
 CASE DESCRIPTION: 'When' with boolean expressions.
 NOTE: for potential analysys on exhaustive by enum of when without bound value.
 */
fun case_2(value_1: _EnumClass) {
    when {
        value_1 == _EnumClass.NORTH -> {}
        value_1 == _EnumClass.SOUTH -> {}
        value_1 == _EnumClass.WEST -> {}
        value_1 == _EnumClass.EAST -> {}
    }
}

/*
 CASE DESCRIPTION: 'When' with boolean expressions.
 NOTE: for potential analysys on exhaustive by boolean of when without bound value.
 */
fun case_3(value_1: Boolean) {
    when {
        value_1 == true -> return
        value_1 == false -> return
    }
}

/*
 CASE DESCRIPTION: 'When' with boolean literals.
 NOTE: for potential mark code after true branch as unreacable.
 */
fun case_4(value_1: Boolean) {
    when {
        false -> return
        true -> return
        value_1 -> return
    }
}

/*
 CASE DESCRIPTION: 'When' with boolean constants.
 NOTE: for potential const propagation use in this case.
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

// CASE DESCRIPTION: 'When' with type checking operator.
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
 CASE DESCRIPTION: 'When' with invert type checking operator.
 NOTE: for potential analysys on exhaustive of when without bound value_1.
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
 CASE DESCRIPTION: 'When' with type checking operator by sealed class.
 NOTE: for potential analysys on exhaustive by sealed class of when without bound value_1.
 */
fun case_8(value_1: _SealedClass) {
    when {
        value_1 is _SealedChild1 -> {}
        value_1 is _SealedChild2 -> {}
        value_1 is _SealedChild3 -> {}
    }
}

// CASE DESCRIPTION: 'When' with containment operator.
fun case_9(value_1: Int, value_2: IntRange) {
    when {
        value_1 in -10..100L -> {}
        value_1 in value_2 -> {}
        value_1 !in listOf(0, 1, 2) -> {}
    }
}
