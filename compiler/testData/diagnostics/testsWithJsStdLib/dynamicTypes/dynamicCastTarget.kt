// !DIAGNOSTICS: -REDUNDANT_NULLABLE

fun test(d: Any, dl: Collection<dynamic>) {
    d as <!DYNAMIC_NOT_ALLOWED!>dynamic<!>
    d as <!DYNAMIC_NOT_ALLOWED!>dynamic?<!>

    d as? <!DYNAMIC_NOT_ALLOWED!>dynamic<!>
    d as? <!DYNAMIC_NOT_ALLOWED!>dynamic?<!>

    <!USELESS_IS_CHECK!>d is <!DYNAMIC_NOT_ALLOWED!>dynamic<!><!>
    <!USELESS_IS_CHECK!>d is <!DYNAMIC_NOT_ALLOWED!>dynamic?<!><!>

    <!USELESS_IS_CHECK!>d !is <!DYNAMIC_NOT_ALLOWED!>dynamic<!><!>
    <!USELESS_IS_CHECK!>d !is <!DYNAMIC_NOT_ALLOWED!>dynamic?<!><!>

    when (d) {
        <!USELESS_IS_CHECK!>is <!DYNAMIC_NOT_ALLOWED!>dynamic<!><!> -> {}
        <!USELESS_IS_CHECK!>is <!DYNAMIC_NOT_ALLOWED, DUPLICATE_LABEL_IN_WHEN!>dynamic?<!><!> -> {}
        <!USELESS_IS_CHECK!>!is <!DYNAMIC_NOT_ALLOWED!>dynamic<!><!> -> {}
        <!USELESS_IS_CHECK!>!is <!DYNAMIC_NOT_ALLOWED, DUPLICATE_LABEL_IN_WHEN!>dynamic?<!><!> -> {}
    }

    dl as List<dynamic>
    <!USELESS_IS_CHECK!>dl is List<dynamic><!>

    when (dl) {
        <!USELESS_IS_CHECK!>is List<dynamic><!> -> {}
    }
}