// ISSUE: KT-58906

fun someFun(i: Int) = 42
fun someSad(i: () -> String) = 42

fun main(args: Array<String>) {
    { <!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>x<!> -> someFun(x) } //here should be CANNOT_INFER_PARAMETER_TYPE
}
