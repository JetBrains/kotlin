
fun test1() {
    <!ARGUMENT_TYPE_MISMATCH!>1<!>. (fun String.(i: Int) = i )(1)
    <!ARGUMENT_TYPE_MISMATCH!>1<!>.(label@ fun String.(i: Int) = i )(1)
}

fun test2(f: String.(Int) -> Unit) {
    <!ARGUMENT_TYPE_MISMATCH!>11<!>.(f)(1)
    11.(f)<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun test3() {
    fun foo(): String.(Int) -> Unit = {}

    <!ARGUMENT_TYPE_MISMATCH!>1<!>.(foo())(1)
}
