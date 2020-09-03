// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: expressions, not-null-assertion-expressions -> paragraph 3 -> sentence 1
 * PRIMARY LINKS: expressions, not-null-assertion-expressions -> paragraph 1 -> sentence 1
 * expressions, not-null-assertion-expressions -> paragraph 1 -> sentence 3
 * type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 9
 * type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 7
 * NUMBER: 1
 * DESCRIPTION: The type of non-null assertion expression is the non-nullable variant of the type of e
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case1(l: Any?) {
    if (l is String?) {
        <!DEBUG_INFO_SMARTCAST!>l<!> checkType { check<String?>() }
        <!DEBUG_INFO_SMARTCAST!>l<!>!!checkType { check<String>() }
        <!DEBUG_INFO_SMARTCAST!>l<!> checkType { check<String>() }
    }
}

// TESTCASE NUMBER: 2
fun case2(l: Any?) {
    if (l is List<*>?) {
        <!DEBUG_INFO_SMARTCAST!>l<!> checkType { check<List<*>?>() }
        <!DEBUG_INFO_SMARTCAST!>l<!>!!checkType { check<List<*>>() }
        <!DEBUG_INFO_SMARTCAST!>l<!> checkType { check<List<*>>() }
    }
}


// TESTCASE NUMBER: 3
fun case3(l: Any?) {
    when (l) {
        is String? -> {
            <!DEBUG_INFO_SMARTCAST!>l<!> checkType { check<String?>() }
            <!DEBUG_INFO_SMARTCAST!>l<!>!!checkType { check<String>() }
            <!DEBUG_INFO_SMARTCAST!>l<!> checkType { check<String>() }
        }
        is List<*>? -> {
            <!DEBUG_INFO_SMARTCAST!>l<!> checkType { check<List<*>?>() }
            <!DEBUG_INFO_SMARTCAST!>l<!>!!checkType { check<List<*>>() }
            <!DEBUG_INFO_SMARTCAST!>l<!> checkType { check<List<*>>() }
        }
    }
}
