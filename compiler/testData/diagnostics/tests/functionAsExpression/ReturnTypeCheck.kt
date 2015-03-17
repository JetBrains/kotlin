val foo = fun(a: Int): String {
    if (a == 1) return "4"
    when (a) {
        5 -> return "2"
        3 -> return <!NULL_FOR_NONNULL_TYPE!>null<!>
        2 -> return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2<!>
    }
    return ""
}