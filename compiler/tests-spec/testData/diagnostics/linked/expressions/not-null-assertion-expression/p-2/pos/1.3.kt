// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: expressions, not-null-assertion-expression -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 7
 * SECONDARY LINKS: type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 8
 * NUMBER: 3
 * DESCRIPTION: If the type of e is non-nullable, not-null assertion expression e!! has no effect.
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case1(l : Any) {
    l as String
    <!DEBUG_INFO_SMARTCAST!>l<!> checkType { check<String>() }
    <!DEBUG_INFO_SMARTCAST!>l<!><!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!> checkType { check<String>() }
    <!DEBUG_INFO_SMARTCAST!>l<!> checkType { check<String>() }
}

// TESTCASE NUMBER: 2
fun case2(l : Any?) {
    l as String
    <!DEBUG_INFO_SMARTCAST!>l<!> checkType { check<String>() }
    <!DEBUG_INFO_SMARTCAST!>l<!><!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!> checkType { check<String>() }
    <!DEBUG_INFO_SMARTCAST!>l<!> checkType { check<String>() }

}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-41575
 */
fun case3(l: Any?) {
    when (l as Any) {
        is String -> {
            <!DEBUG_INFO_SMARTCAST!>l<!> checkType { <!NONE_APPLICABLE!>check<!><String>() }
            l<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!> checkType { <!NONE_APPLICABLE!>check<!><String>() }
            <!DEBUG_INFO_SMARTCAST!>l<!> checkType { <!NONE_APPLICABLE!>check<!><String>() }
        }
        is List<*> -> {
            <!DEBUG_INFO_SMARTCAST!>l<!> checkType { <!NONE_APPLICABLE!>check<!><List<*>>() }
            l<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!> checkType { <!NONE_APPLICABLE!>check<!><List<*>>() }
            <!DEBUG_INFO_SMARTCAST!>l<!> checkType { <!NONE_APPLICABLE!>check<!><List<*>>() }
        }
    }
}
