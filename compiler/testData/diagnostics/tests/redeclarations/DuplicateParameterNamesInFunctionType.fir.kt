// RUN_PIPELINE_TILL: FRONTEND
fun test0(f: (String, String) -> Unit) {
    f("", "")
}

fun test1(f: <!DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE!>(a: Int, a: Int) -> Unit<!>) {
    f(1, 1)
}

fun test2(f: <!DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE!>(@ParameterName("a") Int, @ParameterName("a") Int) -> Unit<!>) {
    f(1, 1)
}

fun test3(f: <!DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE, REPEATED_ANNOTATION!>(@ParameterName("a") @ParameterName("b") Int, @ParameterName("a") Int) -> Unit<!>) {
    f(1, 1)
}

fun test4(f: <!REPEATED_ANNOTATION!>(@ParameterName("b") @ParameterName("a") Int, @ParameterName("a") Int) -> Unit<!>) {
    f(1, 1)
}
