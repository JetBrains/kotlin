// !DIAGNOSTICS: -REDUNDANT_NULLABLE

fun test(d: Any, dl: Collection<dynamic>) {
    d as <!DYNAMIC_NOT_ALLOWED!>dynamic<!>
    d as <!DYNAMIC_NOT_ALLOWED!>dynamic?<!>

    d as? <!DYNAMIC_NOT_ALLOWED!>dynamic<!>
    d as? <!DYNAMIC_NOT_ALLOWED!>dynamic?<!>

    d is <!DYNAMIC_NOT_ALLOWED!>dynamic<!>
    d is <!DYNAMIC_NOT_ALLOWED!>dynamic?<!>

    d !is <!DYNAMIC_NOT_ALLOWED!>dynamic<!>
    d !is <!DYNAMIC_NOT_ALLOWED!>dynamic?<!>

    when (d) {
        is <!DYNAMIC_NOT_ALLOWED!>dynamic<!> -> {}
        is <!DYNAMIC_NOT_ALLOWED, DUPLICATE_LABEL_IN_WHEN!>dynamic?<!> -> {}
        !is <!DYNAMIC_NOT_ALLOWED!>dynamic<!> -> {}
        !is <!DYNAMIC_NOT_ALLOWED, DUPLICATE_LABEL_IN_WHEN!>dynamic?<!> -> {}
    }

    dl as List<dynamic>
    dl is List<dynamic>

    when (dl) {
        is List<dynamic> -> {}
    }
}