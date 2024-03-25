val foo = fun(a: Int): String {
    if (a == 1) return "4"
    when (a) {
        5 -> return "2"
        3 -> return <!NULL_FOR_NONNULL_TYPE!>null<!>
        2 -> return <!RETURN_TYPE_MISMATCH!>2<!>
    }
    return ""
}

val bar: (Int) -> String = <!INITIALIZER_TYPE_MISMATCH!>l@{ a ->
    if (a == 1) return@l "4"
    when (a) {
        5 -> return@l "2"
        3 -> return@l null
        2 -> return@l 2
    }
    return@l ""
}<!>
