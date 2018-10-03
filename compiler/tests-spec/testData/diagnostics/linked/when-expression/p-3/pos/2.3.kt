// !WITH_BASIC_TYPES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTIONS: when-expression
 PARAGRAPH: 3
 SENTENCE: [2] Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
 NUMBER: 3
 DESCRIPTION: 'When' without bound value and with Nothing in condition (subtype of Boolean).
 DISCUSSION
 ISSUES: KT-25948
 */

// CASE DESCRIPTION: 'When' with return expression in condition.
fun case_1(<!UNUSED_PARAMETER!>value_1<!>: _BasicTypesProvider) {
    when {
        return -> <!UNREACHABLE_CODE!>return<!>
        <!UNREACHABLE_CODE!>return == return -> return<!>
        <!UNREACHABLE_CODE!>return return return -> return<!>
        <!UNREACHABLE_CODE!>return != 10L -> return<!>
        <!UNREACHABLE_CODE!>return || return && return -> return<!>
    }
}

// CASE DESCRIPTION: 'When' with throw expression in condition.
fun case_2(<!UNUSED_PARAMETER!>value_1<!>: _BasicTypesProvider) {
    when {
        throw Exception() -> <!UNREACHABLE_CODE!>return<!>
        <!UNREACHABLE_CODE!>(throw Exception()) == (throw Exception()) -> return<!>
        <!UNREACHABLE_CODE!>(throw Exception()) && (throw Exception()) || (throw Exception()) -> return<!>
        <!UNREACHABLE_CODE!>(throw Exception()) == 10L -> return<!>
        <!UNREACHABLE_CODE!>throw throw throw throw Exception() -> return<!>
    }
}

// CASE DESCRIPTION: 'When' with break expression in condition.
fun case_3(<!UNUSED_PARAMETER!>value_1<!>: _BasicTypesProvider) {
    loop1@ while (true) {
        loop2@ while (true) {
            loop3@ while (true) {
                when {
                    break@loop1 <!UNREACHABLE_CODE!>== break@loop2<!> -> <!UNREACHABLE_CODE!>return<!>
                    <!UNREACHABLE_CODE!>break@loop2 || break@loop1 && break@loop3 -> return<!>
                    <!UNREACHABLE_CODE!>break@loop2 != 10L -> return<!>
                }
            }
        }
    }
}

// CASE DESCRIPTION: 'When' with continue expression in condition.
fun case_4(<!UNUSED_PARAMETER!>value_1<!>: _BasicTypesProvider): String {
    loop1@ while (true) {
        loop2@ while (true) {
            loop3@ while (true) {
                when {
                    continue@loop1 <!UNREACHABLE_CODE!>== continue@loop2<!> -> <!UNREACHABLE_CODE!>return ""<!>
                    <!UNREACHABLE_CODE!>continue@loop2 || continue@loop1 && continue@loop3 -> return ""<!>
                    <!UNREACHABLE_CODE!>continue@loop2 != 10L -> return ""<!>
                }
            }
        }
    }
}

// CASE DESCRIPTION: 'When' with values of Nothing type.
fun case_6(value_1: Nothing, <!UNUSED_PARAMETER!>value_2<!>: _BasicTypesProvider): String {
    when {
        value_1 -> <!UNREACHABLE_CODE!>return ""<!>
        <!UNREACHABLE_CODE!>value_2.getNothing() -> return ""<!>
        <!UNREACHABLE_CODE!>getNothing() -> return ""<!>
        <!UNREACHABLE_CODE!>value_1 && (getNothing() == value_2.getNothing()) -> return ""<!>
    }

    <!UNREACHABLE_CODE!>return ""<!>
}

// CASE DESCRIPTION: 'When' with mixed Nothing expression in condition.
fun case_5(<!UNUSED_PARAMETER!>value_1<!>: _BasicTypesProvider, <!UNUSED_PARAMETER!>value_2<!>: Nothing) {
    loop1@ while (true) {
        loop2@ while (true) {
            loop3@ while (true) {
                when {
                    continue@loop1 <!UNREACHABLE_CODE!>== throw throw throw throw Exception()<!> -> <!UNREACHABLE_CODE!>return<!>
                    <!UNREACHABLE_CODE!>(return return return return) || break@loop1 && break@loop3 -> return<!>
                    <!UNREACHABLE_CODE!>continue@loop1 != 10L && (return return) == continue@loop1 -> return<!>
                    <!UNREACHABLE_CODE!>return continue@loop1 -> return<!>
                    <!UNREACHABLE_CODE!>(throw break@loop1) && break@loop3 -> return<!>
                    <!UNREACHABLE_CODE!>(throw getNothing()) && value_1.getNothing() -> return<!>
                    <!UNREACHABLE_CODE!>return return return value_2 -> return<!>
                    <!UNREACHABLE_CODE!>getNothing() != 10L && (return return) == value_2 -> return<!>
                }
            }
        }
    }
}
