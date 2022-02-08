// ISSUE: KT-50785

fun test_1(s: String?) {
    when (true) {
        (s != null) -> s.length
        else -> null
    }
}

fun test_2(s: String?) {
    when (s != null) {
        true -> s.length
        else -> null
    }
}

fun test_3(s: String?) {
    if (true == (s != null)) s.length
}
