/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 3
 DESCRIPTION: 'When' with different variants of the equality expression in the control structure body.
 */

fun case_1(
    value: Int,
    value1: Boolean,
    value2: Byte,
    value3: Short,
    value4: Int,
    value5: Long,
    value6: Float,
    value7: Double,
    value8: String,
    value9: Char,
    obj1: List<String>,
    obj2: List<String>,
    obj3: Nothing,
    obj4: Any
) {
    when (value) {
        1 -> value1 == true
        2 -> value2 == 127.toByte()
        3 -> value3 != 11.toShort()
        4 -> value4 != 13142
        5 -> value5 == 1243124124431443L
        6 -> value6 != .0000000012f
        7 -> value7 == 13223.12391293
        8 -> value8 == ""
        9 -> value9 != 'a'
        10 -> {
            obj2 === obj1 && obj4 != obj2 || !(value6 == .000023412f) && value9 == '0' || value1 == false
        }
        11 -> obj3 <!UNREACHABLE_CODE!>=== obj4<!>
        12 -> obj4 <!UNREACHABLE_CODE!>!==<!> obj3
        13 -> obj3 <!UNREACHABLE_CODE!>!= obj3<!>
        14 -> obj1 === obj2
        15 -> obj1 == obj2
    }
}