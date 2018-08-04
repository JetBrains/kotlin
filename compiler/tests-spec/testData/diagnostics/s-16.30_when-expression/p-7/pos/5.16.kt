// !WITH_CLASSES

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 16
 DESCRIPTION: 'When' with bound value and property access expressions in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with 'else' branch (as expression).
fun case_1(value: Int?, value1: _Class, value2: _Class?): String = when (value) {
    value1.prop_1 -> ""
    value2?.prop_2 -> ""
    value2!!::prop_2.get() -> ""
    value1::prop_2.get() -> ""
    (_Class::_NestedClass)()::prop_4.get() -> ""
    (_Class::_NestedClass)().prop_4 -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement).
fun case_2(value: Int?, value1: _Class, value2: _Class?): String {
    when (value) {
        value1.prop_1 -> return ""
        value2?.prop_2 -> return ""
        value2!!::prop_2.get() -> return ""
        value1::prop_2.get() -> return ""
        (_Class::_NestedClass)()::prop_4.get() -> return ""
        (_Class::_NestedClass)().prop_4 -> return ""
    }

    return ""
}