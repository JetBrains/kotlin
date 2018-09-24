// !WITH_BASIC_TYPES
// !WITH_SEALED_CLASSES
// !WITH_ENUM_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 3
 SENTENCE: [2] Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
 NUMBER: 1
 DESCRIPTION: 'When' without bound value and different variants of the boolean conditions (logical, equality, comparison, type checking operator, containment operator).
 */

// CASE DESCRIPTION: 'When' with boolean expressions and else branch.
fun case_1(value1: Boolean, value2: Long): Int {
    return when {
        value1 -> 1
        getBoolean() && value1 -> 2
        getChar(10) != 'a' -> 3
        getList() === getAny() -> 4
        value2 <= 11 -> 5
        !value1 -> 6
        else -> 7
    }
}

/*
 CASE DESCRIPTION: 'When' with boolean expressions.
 NOTE: for potential analysys on exhaustive by enum of when without bound value.
 */
fun case_2(value: _EnumClass) {
    when {
        value == _EnumClass.NORTH -> {}
        value == _EnumClass.SOUTH -> {}
        value == _EnumClass.WEST -> {}
        value == _EnumClass.EAST -> {}
    }
}

/*
 CASE DESCRIPTION: 'When' with boolean expressions.
 NOTE: for potential analysys on exhaustive by boolean of when without bound value.
 */
fun case_3(value: Boolean) {
    when {
        value == true -> return
        value == false -> return
    }
}

/*
 CASE DESCRIPTION: 'When' with boolean literals.
 NOTE: for potential mark code after true branch as unreacable.
 */
fun case_4(value1: Boolean) {
    when {
        false -> return
        true -> return
        value1 -> return
    }
}

/*
 CASE DESCRIPTION: 'When' with boolean constants.
 NOTE: for potential const propagation use in this case.
 */
fun case_5(value1: Boolean) {
    val value2 = false
    val value3 = false || !!!false || false

    when {
        value3 -> return
        value2 -> return
        value1 -> return
    }
}

// CASE DESCRIPTION: 'When' with type checking operator.
fun case_6(value: Any) {
    when {
        value is Nothing -> {}
        value is Int -> {}
        value is Boolean -> {}
        value is String -> {}
        value is Number -> {}
        value is Float -> {}
        <!USELESS_IS_CHECK!>value is Any<!> -> {}
    }
}

/*
 CASE DESCRIPTION: 'When' with invert type checking operator.
 NOTE: for potential analysys on exhaustive of when without bound value.
 */
fun case_7(value: Any) {
    when {
        value !is Number -> {}
        value is Float -> {}
        <!USELESS_IS_CHECK!>value is Number<!> -> {}
        <!USELESS_IS_CHECK!>value is Any<!> -> {}
    }
}

/*
 CASE DESCRIPTION: 'When' with type checking operator by sealed class.
 NOTE: for potential analysys on exhaustive by sealed class of when without bound value.
 */
fun case_8(value: _SealedClass) {
    when {
        value is _SealedChild1 -> {}
        value is _SealedChild2 -> {}
        value is _SealedChild3 -> {}
    }
}

// CASE DESCRIPTION: 'When' with containment operator.
fun case_9(value: Int, value1: IntRange) {
    when {
        value in -10..100L -> {}
        value in value1 -> {}
        value !in listOf(0, 1, 2) -> {}
    }
}
