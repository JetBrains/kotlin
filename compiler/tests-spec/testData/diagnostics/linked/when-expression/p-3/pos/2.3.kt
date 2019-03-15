
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 3 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: 'When' without bound value and with Nothing in condition (subtype of Boolean).
 * DISCUSSION
 * ISSUES: KT-25948
 * HELPERS: typesProvider
 */

// TESTCASE NUMBER: 1
fun case_1(<!UNUSED_PARAMETER!>value_1<!>: TypesProvider) {
    when {
        return -> <!UNREACHABLE_CODE!>return<!>
        <!UNREACHABLE_CODE!>return == return -> return<!>
        <!UNREACHABLE_CODE!>return return return -> return<!>
        <!UNREACHABLE_CODE!>return != 10L -> return<!>
        <!UNREACHABLE_CODE!>return || return && return -> return<!>
    }
}

// TESTCASE NUMBER: 2
fun case_2(<!UNUSED_PARAMETER!>value_1<!>: TypesProvider) {
    when {
        throw Exception() -> <!UNREACHABLE_CODE!>return<!>
        <!UNREACHABLE_CODE!>(throw Exception()) == (throw Exception()) -> return<!>
        <!UNREACHABLE_CODE!>(throw Exception()) && (throw Exception()) || (throw Exception()) -> return<!>
        <!UNREACHABLE_CODE!>(throw Exception()) == 10L -> return<!>
        <!UNREACHABLE_CODE!>throw throw throw throw Exception() -> return<!>
    }
}

// TESTCASE NUMBER: 3
fun case_3(<!UNUSED_PARAMETER!>value_1<!>: TypesProvider) {
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

// TESTCASE NUMBER: 4
fun case_4(<!UNUSED_PARAMETER!>value_1<!>: TypesProvider): String {
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

// TESTCASE NUMBER: 6
fun case_6(value_1: Nothing, <!UNUSED_PARAMETER!>value_2<!>: TypesProvider): String {
    when {
        value_1 -> <!UNREACHABLE_CODE!>return ""<!>
        <!UNREACHABLE_CODE!>value_2.getNothing() -> return ""<!>
        <!UNREACHABLE_CODE!>getNothing() -> return ""<!>
        <!UNREACHABLE_CODE!>value_1 && (getNothing() == value_2.getNothing()) -> return ""<!>
    }

    <!UNREACHABLE_CODE!>return ""<!>
}

// TESTCASE NUMBER: 5
fun case_5(<!UNUSED_PARAMETER!>value_1<!>: TypesProvider, <!UNUSED_PARAMETER!>value_2<!>: Nothing) {
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
