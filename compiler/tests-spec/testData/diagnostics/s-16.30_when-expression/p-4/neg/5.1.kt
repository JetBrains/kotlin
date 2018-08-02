
/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 5: Contains test condition: containment operator followed by an expression.
 NUMBER: 1
 DESCRIPTION: 'When' with bound value and 'when condition' with range expression, but without contains operator.
 */

fun case_1(value: Int): String {
    when (value) {
        <!INCOMPATIBLE_TYPES!>0..10<!> -> return ""
        <!INCOMPATIBLE_TYPES!>-10..10<!> -> return ""
        <!INCOMPATIBLE_TYPES!>-0..0<!> -> return ""
    }

    return ""
}
