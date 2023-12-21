// !DIAGNOSTICS: -REDUNDANT_NULLABLE

fun test(d: Any, dl: Collection<dynamic>) {
    d <!USELESS_CAST!>as <!DYNAMIC_NOT_ALLOWED!>dynamic<!><!>
    d <!USELESS_CAST!>as <!DYNAMIC_NOT_ALLOWED!>dynamic?<!><!>

    d <!USELESS_CAST!>as? <!DYNAMIC_NOT_ALLOWED!>dynamic<!><!>
    d <!USELESS_CAST!>as? <!DYNAMIC_NOT_ALLOWED!>dynamic?<!><!>

    <!USELESS_IS_CHECK!>d is <!DYNAMIC_NOT_ALLOWED!>dynamic<!><!>
    <!USELESS_IS_CHECK!>d is <!DYNAMIC_NOT_ALLOWED!>dynamic?<!><!>

    <!USELESS_IS_CHECK!>d !is <!DYNAMIC_NOT_ALLOWED!>dynamic<!><!>
    <!USELESS_IS_CHECK!>d !is <!DYNAMIC_NOT_ALLOWED!>dynamic?<!><!>

    when (d) {
        <!USELESS_IS_CHECK!>is <!DYNAMIC_NOT_ALLOWED!>dynamic<!><!> -> {}
        <!USELESS_IS_CHECK!>is <!DUPLICATE_BRANCH_CONDITION_IN_WHEN, DYNAMIC_NOT_ALLOWED!>dynamic?<!><!> -> {}
        <!USELESS_IS_CHECK!>!is <!DYNAMIC_NOT_ALLOWED!>dynamic<!><!> -> {}
        <!USELESS_IS_CHECK!>!is <!DUPLICATE_BRANCH_CONDITION_IN_WHEN, DYNAMIC_NOT_ALLOWED!>dynamic?<!><!> -> {}
    }

    dl as List<dynamic>
    <!USELESS_IS_CHECK!>dl is List<dynamic><!>

    when (dl) {
        <!USELESS_IS_CHECK!>is List<dynamic><!> -> {}
    }
}
