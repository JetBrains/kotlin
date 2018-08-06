
/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 7: Any other expression.
 NUMBER: 23
 DESCRIPTION: 'When' with bound value and continue expression in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with 'else' branch (as expression) and only one continue expression.
fun case_1(value: Any?, value1: MutableList<Int>): String {
    loop@ while (value1.isNotEmpty()) {
        value1.removeAt(0)
        when (value) {
            continue@loop<!UNREACHABLE_CODE!><!> -> <!UNREACHABLE_CODE!>return ""<!>
        }
    }

    return ""
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement) and only one continue expression.
fun case_2(value: Any?, value1: MutableList<Int>): String? {
    var whenValue: String? = null

    loop@ while (value1.isNotEmpty()) {
        value1.removeAt(0)
        <!UNREACHABLE_CODE!>whenValue = when (<!>value<!UNREACHABLE_CODE!>) {<!>
            continue@loop <!UNREACHABLE_CODE!>-> ""
            else -> ""
        }<!>
    }

    return whenValue
}

// CASE DESCRIPTION: 'When' with 'else' branch (as expression) and several continue expressions.
fun case_3(value: Any?, value1: MutableList<Int>, value2: MutableList<Int>, value3: MutableList<Int>, value4: MutableList<Int>): String {
    loop1@ while (value1.isNotEmpty()) {
        loop2@ while (value2.isNotEmpty()) {
            loop3@ while (value3.isNotEmpty()) {
                loop4@ while (value4.isNotEmpty()) {
                    value4.removeAt(0)
                    when (value) {
                        continue@loop1<!UNREACHABLE_CODE!><!> -> <!UNREACHABLE_CODE!>return ""<!>
                        <!UNREACHABLE_CODE!>continue@loop2 -> return ""<!>
                        <!UNREACHABLE_CODE!>continue@loop3 -> return ""<!>
                        <!UNREACHABLE_CODE!>continue@loop4 -> return ""<!>
                    }
                }
                value3.removeAt(0)
            }
            value3.removeAt(0)
        }
        value1.removeAt(0)
    }

    return ""
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement) and several continue expressions.
fun case_4(value: Any?, value1: MutableList<Int>, value2: MutableList<Int>, value3: MutableList<Int>, value4: MutableList<Int>): String? {
    var whenValue: String? = null

    loop1@ while (value1.isNotEmpty()) {
        loop2@ while (value2.isNotEmpty()) {
            loop3@ while (!value3.isNotEmpty()) {
                loop4@ while (value4.isNotEmpty()) {
                    value4.removeAt(0)
                    <!UNREACHABLE_CODE!>whenValue = when (<!>value<!UNREACHABLE_CODE!>) {<!>
                        continue@loop1 <!UNREACHABLE_CODE!>-> ""
                        continue@loop2 -> ""
                        continue@loop3 -> ""
                        continue@loop4 -> ""
                    }<!>
                }
                value3.removeAt(0)
            }
            value2.removeAt(0)
        }
        value1.removeAt(0)
    }

    return whenValue
}