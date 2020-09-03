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
 * type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 8
 * type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 7
 * NUMBER: 2
 * DESCRIPTION: The type of non-null assertion expression is the non-nullable variant of the type of e
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case1(l: Any?) {
    l as String? 
    <!DEBUG_INFO_SMARTCAST!>l<!> checkType { check<String?>() }
    <!DEBUG_INFO_SMARTCAST!>l<!>!!checkType { check<String>() }
    <!DEBUG_INFO_SMARTCAST!>l<!> checkType { check<String>() }
}

// TESTCASE NUMBER: 2
fun case2(l: Any?) {
    l as List<*>?
    <!DEBUG_INFO_SMARTCAST!>l<!> checkType { check<List<*>?>() }
    <!DEBUG_INFO_SMARTCAST!>l<!>!!checkType { check<List<*>>() }
    <!DEBUG_INFO_SMARTCAST!>l<!> checkType { check<List<*>>() }
}


/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-41575
 */
fun case3(l: Any?) {
    when (l) {
        l as CharSequence? is String? -> {
            <!DEBUG_INFO_SMARTCAST!>l<!> checkType { <!NONE_APPLICABLE!>check<!><String?>() }
            <!DEBUG_INFO_SMARTCAST!>l<!><!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!> checkType { <!NONE_APPLICABLE!>check<!><String>() }
            <!DEBUG_INFO_SMARTCAST!>l<!> checkType { <!NONE_APPLICABLE!>check<!><String>() }
        }
        l as List<*>? is MutableList<*> -> {
            <!DEBUG_INFO_SMARTCAST!>l<!> checkType { <!NONE_APPLICABLE!>check<!><List<*>?>() }
            l<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!> checkType { <!NONE_APPLICABLE!>check<!><List<*>>() }
            <!DEBUG_INFO_SMARTCAST!>l<!> checkType { <!NONE_APPLICABLE!>check<!><List<*>>() }
        }
    }
}
