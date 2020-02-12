fun <X> test_1(a: X) {
    if (a is String?) {
        <!INAPPLICABLE_CANDIDATE!>takeString<!>(a!!)
    }
}

fun takeString(s: String) {}