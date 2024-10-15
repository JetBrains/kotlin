// RUN_PIPELINE_TILL: FRONTEND
fun test0(f: (String, String) -> Unit) {
    f("", "")
}

fun test1(f: (<!DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE!>a<!>: Int, <!DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE!>a<!>: Int) -> Unit) {
    f(1, 1)
}

fun test2(f: (@ParameterName("a") Int, @ParameterName("a") Int) -> Unit) {
    f(1, 1)
}

fun test3(f: (@ParameterName("a") @ParameterName("b") Int, @ParameterName("a") Int) -> Unit) {
    f(1, 1)
}

fun test4(f: (@ParameterName("b") @ParameterName("a") Int, @ParameterName("a") Int) -> Unit) {
    f(1, 1)
}
