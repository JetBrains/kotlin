// !WITH_BASIC_TYPES
// !WITH_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 7
 SENTENCE: [3] Contains test condition: containment operator followed by an expression.
 NUMBER: 2
 DESCRIPTION: 'When' with bound value and enumeration of the containment operators.
 */

// CASE DESCRIPTION: 'When' with range operator.
fun case_1(value: Int, value1: Int, value2: Short): String {
    when (value) {
        in Long.MIN_VALUE..-100, in -99..0 -> return ""
        !in 100.toByte()..value1, in value1..value2 -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' on types with contains method defined.
fun case_2(value: Int, value1: List<IntArray>, value2: _Class) = when (value) {
    !in value1[0], !in listOf(0, 1, 2, 3, 4), !in value2.getIntArray(90) -> ""
    else -> ""
}