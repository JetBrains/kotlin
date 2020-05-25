// TESTCASE NUMBER: 1
fun case_1() {
    true checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Boolean?>() }
    false checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Boolean?>() }

    true checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Any?>() }
    false checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Any>() }

    true checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Nothing?>() }
    false checkType { <!INAPPLICABLE_CANDIDATE!>check<!><Nothing>() }
}
