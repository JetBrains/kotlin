// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: expressions, when-expression -> paragraph 2 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: 'When' without bound value and with Nothing in condition (subtype of Boolean).
 * DISCUSSION
 * ISSUES: KT-25948
 * HELPERS: typesProvider
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: TypesProvider) {
    when {
        return -> return
        return == return -> return
        return return return -> return
        return != 10L -> return
        return || return && return -> return
    }
}

// TESTCASE NUMBER: 2
fun case_2(value_1: TypesProvider) {
    when {
        throw Exception() -> return
        (throw Exception()) == (throw Exception()) -> return
        (throw Exception()) && (throw Exception()) || (throw Exception()) -> return
        (throw Exception()) == 10L -> return
        throw throw throw throw Exception() -> return
    }
}

// TESTCASE NUMBER: 3
fun case_3(value_1: TypesProvider) {
    loop1@ while (true) {
        loop2@ while (true) {
            loop3@ while (true) {
                when {
                    break@loop1 == break@loop2 -> return
                    break@loop2 || break@loop1 && break@loop3 -> return
                    break@loop2 != 10L -> return
                }
            }
        }
    }
}

// TESTCASE NUMBER: 4
fun case_4(value_1: TypesProvider): String {
    loop1@ while (true) {
        loop2@ while (true) {
            loop3@ while (true) {
                when {
                    continue@loop1 == continue@loop2 -> return ""
                    continue@loop2 || continue@loop1 && continue@loop3 -> return ""
                    continue@loop2 != 10L -> return ""
                }
            }
        }
    }
}

// TESTCASE NUMBER: 6
fun case_6(value_1: Nothing, value_2: TypesProvider): String {
    when {
        value_1 -> return ""
        value_2.getNothing() -> return ""
        getNothing() -> return ""
        value_1 && (getNothing() == value_2.getNothing()) -> return ""
    }

    return ""
}

// TESTCASE NUMBER: 5
fun case_5(value_1: TypesProvider, value_2: Nothing) {
    loop1@ while (true) {
        loop2@ while (true) {
            loop3@ while (true) {
                when {
                    continue@loop1 == throw throw throw throw Exception() -> return
                    (return return return return) || break@loop1 && break@loop3 -> return
                    continue@loop1 != 10L && (return return) == continue@loop1 -> return
                    return continue@loop1 -> return
                    (throw break@loop1) && break@loop3 -> return
                    (throw getNothing()) && value_1.getNothing() -> return
                    return return return value_2 -> return
                    getNothing() != 10L && (return return) == value_2 -> return
                }
            }
        }
    }
}
