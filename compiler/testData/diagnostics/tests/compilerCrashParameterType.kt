// ISSUE: KT-58906

fun someFun(i: Int) = 42
fun someSad(i: () -> String) = 42

fun main(args: Array<String>) {
    { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> -> someFun(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>) } //here should be CANNOT_INFER_PARAMETER_TYPE
}
