
fun test1() {
    1. (<!FUNCTION_EXPECTED!>fun String.(i: Int) = i<!> )(1)
    1.(<!FUNCTION_EXPECTED!>label@ fun String.(i: Int) = i<!> )(1)
}

fun test2(f: String.(Int) -> Unit) {
    <!ARGUMENT_TYPE_MISMATCH!>11<!>.(f)(1)
    11.(f)(<!NO_VALUE_FOR_PARAMETER!>)<!>
}

fun test3() {
    fun foo(): String.(Int) -> Unit = {}

    1.(<!FUNCTION_EXPECTED!>foo()<!>)(1)
}
