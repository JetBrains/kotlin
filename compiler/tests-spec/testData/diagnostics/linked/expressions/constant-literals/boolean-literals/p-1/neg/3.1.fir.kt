// TESTCASE NUMBER: 1
fun case_1() {
    true checkType { <!NONE_APPLICABLE!>check<!><Boolean?>() }
    false checkType { <!NONE_APPLICABLE!>check<!><Boolean?>() }

    true checkType { <!NONE_APPLICABLE!>check<!><Any?>() }
    false checkType { <!NONE_APPLICABLE!>check<!><Any>() }

    true checkType { <!NONE_APPLICABLE!>check<!><Nothing?>() }
    false checkType { <!NONE_APPLICABLE!>check<!><Nothing>() }
}
