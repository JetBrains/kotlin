// ISSUE: KT-54920

fun testCharSequence(x: CharSequence): Int {
    var i = 0

    i += when (x) { <!USELESS_IS_CHECK!>is Any<!> -> 1 }
    i += when (x) { <!USELESS_IS_CHECK!>is CharSequence<!> -> 1 }
    i += <!NO_ELSE_IN_WHEN!>when<!> (x) { is String -> 1 }

    i += when (x) { <!USELESS_IS_CHECK!>is Any?<!> -> 1 }
    i += when (x) { <!USELESS_IS_CHECK!>is CharSequence?<!> -> 1 }
    i += <!NO_ELSE_IN_WHEN!>when<!> (x) { is String? -> 1 }

    return i
}

fun testNullableCharSequence(x: CharSequence?): Int {
    var i = 0

    i += <!NO_ELSE_IN_WHEN!>when<!> (x) { is Any -> 1 }
    i += <!NO_ELSE_IN_WHEN!>when<!> (x) { is CharSequence -> 1 }
    i += <!NO_ELSE_IN_WHEN!>when<!> (x) { is String -> 1 }

    i += when (x) { <!USELESS_IS_CHECK!>is Any?<!> -> 1 }
    i += when (x) { <!USELESS_IS_CHECK!>is CharSequence?<!> -> 1 }
    i += <!NO_ELSE_IN_WHEN!>when<!> (x) { is String? -> 1 }

    i += when (x) {
        is Any -> 1
        <!SENSELESS_COMPARISON!>null<!> -> 2
    }
    i += when (x) {
        is CharSequence -> 1
        null -> 2
    }
    i += <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is String -> 1
        null -> 2
    }

    return i
}
