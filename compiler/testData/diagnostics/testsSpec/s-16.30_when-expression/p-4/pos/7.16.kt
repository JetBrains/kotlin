// SKIP_TXT

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 7: Any other expression.
 NUMBER: 16
 DESCRIPTION: 'When' with bound value and property access expressions in 'when condition'.
 */

class A {
    val prop1 = 1
    var prop2 = 2
    class B {
        val prop4 = 3
        val prop5 = 3
    }
}

// CASE DESCRIPTION: 'When' with 'else' branch (as expression).
fun case_1(value: Int?, value1: A, value2: A?): String = when (value) {
    value1.prop1 -> ""
    value2?.prop2 -> ""
    value2!!::prop2.get() -> ""
    value1::prop2.get() -> ""
    (A::B)()::prop4.get() -> ""
    (A::B)().prop4 -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement).
fun case_2(value: Int?, value1: A, value2: A?): String {
    when (value) {
        value1.prop1 -> return ""
        value2?.prop2 -> return ""
        value2!!::prop2.get() -> return ""
        value1::prop2.get() -> return ""
        (A::B)()::prop4.get() -> return ""
        (A::B)().prop4 -> return ""
    }

    return ""
}