fun test_1(array: Array<String>) {
    array.<!UNRESOLVED_REFERENCE!>clone<!>()
}

fun test_2(array: IntArray) {
    array.<!UNRESOLVED_REFERENCE!>clone<!>()
}