// !WITH_BASIC_TYPES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 3
 SENTENCE: [2] Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
 NUMBER: 3
 DESCRIPTION: 'When' without bound value and with Nothing in condition (subtype of Boolean).
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-25948
 */

// CASE DESCRIPTION: 'When' with return expression in condition.
fun case_1(<!UNUSED_PARAMETER!>value1<!>: _BasicTypesProvider) {
    when {
        return -> <!UNREACHABLE_CODE!>return<!>
        <!UNREACHABLE_CODE!>return == return -> return<!>
        <!UNREACHABLE_CODE!>return return return -> return<!>
        <!UNREACHABLE_CODE!>return != 10L -> return<!>
        <!UNREACHABLE_CODE!>return || return && return -> return<!>
    }
}

// CASE DESCRIPTION: 'When' with throw expression in condition.
fun case_2(<!UNUSED_PARAMETER!>value1<!>: _BasicTypesProvider) {
    when {
        throw Exception() -> <!UNREACHABLE_CODE!>return<!>
        <!UNREACHABLE_CODE!>(throw Exception()) == (throw Exception()) -> return<!>
        <!UNREACHABLE_CODE!>(throw Exception()) && (throw Exception()) || (throw Exception()) -> return<!>
        <!UNREACHABLE_CODE!>(throw Exception()) == 10L -> return<!>
        <!UNREACHABLE_CODE!>throw throw throw throw Exception() -> return<!>
    }
}

// CASE DESCRIPTION: 'When' with break expression in condition.
fun case_3(<!UNUSED_PARAMETER!>value1<!>: _BasicTypesProvider) {
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
fun case_4(<!UNUSED_PARAMETER!>value1<!>: _BasicTypesProvider): String {
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
fun case_6(value1: Nothing, <!UNUSED_PARAMETER!>value2<!>: _BasicTypesProvider): String {
    when {
        value1 -> <!UNREACHABLE_CODE!>return ""<!>
        <!UNREACHABLE_CODE!>value2.getNothing() -> return ""<!>
        <!UNREACHABLE_CODE!>getNothing() -> return ""<!>
        <!UNREACHABLE_CODE!>value1 && (getNothing() == value2.getNothing()) -> return ""<!>
    }

    <!UNREACHABLE_CODE!>return ""<!>
}

// CASE DESCRIPTION: 'When' with mixed Nothing expression in condition.
fun case_5(<!UNUSED_PARAMETER!>value1<!>: _BasicTypesProvider, <!UNUSED_PARAMETER!>value2<!>: Nothing) {
    loop1@ while (true) {
        loop2@ while (true) {
            loop3@ while (true) {
                when {
                    continue@loop1 <!UNREACHABLE_CODE!>== throw throw throw throw Exception()<!> -> <!UNREACHABLE_CODE!>return<!>
                    <!UNREACHABLE_CODE!>(return return return return) || break@loop1 && break@loop3 -> return<!>
                    <!UNREACHABLE_CODE!>continue@loop1 != 10L && (return return) == continue@loop1 -> return<!>
                    <!UNREACHABLE_CODE!>return continue@loop1 -> return<!>
                    <!UNREACHABLE_CODE!>(throw break@loop1) && break@loop3 -> return<!>
                    <!UNREACHABLE_CODE!>(throw getNothing()) && value1.getNothing() -> return<!>
                    <!UNREACHABLE_CODE!>return return return value2 -> return<!>
                    <!UNREACHABLE_CODE!>getNothing() != 10L && (return return) == value2 -> return<!>
                }
            }
        }
    }
}
