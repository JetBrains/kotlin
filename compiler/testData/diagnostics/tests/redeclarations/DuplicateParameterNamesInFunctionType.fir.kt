fun test0(f: (String, String) -> Unit) {
    f("", "")
}

fun test1(f: <!DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE!>(a: Int, a: Int) -> Unit<!>) {
    f(1, 1)
}