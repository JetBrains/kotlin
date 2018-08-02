/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 4
 DESCRIPTION: 'When' with different variants of the comparison expression in the control structure body.
 */

fun case_1(
    value: Int,
    value1: Byte,
    value2: Short,
    value3: Int,
    value4: Long,
    value5: Float,
    value6: Double,
    value7: Char
) {
    when {
        value == 1 -> value1 >= 100
        value == 2 -> value2 < 32000
        value == 3 -> value3 <= 11
        value == 4 -> value4 > 1243124124431443L
        value == 5 -> value5 < -.000001f
        value == 6 -> value6 >= 10.0
        value == 7 -> value7 <= 254.toChar()
        value == 15 -> {
            value1 < -100 || value2 > 10 && value3 <= 320302 || !(value4 > -0L) && value5 <= .1F || !!!(value6 > -.999999999) && value7 < 10.toChar()
        }
    }
}