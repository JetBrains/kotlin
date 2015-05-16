fun test1() {
    <!TYPE_MISMATCH!>1<!>. (fun String.(i: Int) = i )(1)
    <!TYPE_MISMATCH!>1<!>.(label@ fun String.(i: Int) = i )(1)
}

fun test2(f: String.(Int) -> Unit) {
    <!TYPE_MISMATCH!>11<!>.(f)(1)
    <!TYPE_MISMATCH!>11<!>.(f)(<!NO_VALUE_FOR_PARAMETER!>)<!>
}

fun test3() {
    fun foo(): String.(Int) -> Unit = {}

    <!TYPE_MISMATCH!>1<!>.(foo())(1)
}