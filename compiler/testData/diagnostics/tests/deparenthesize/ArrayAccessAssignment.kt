package t

fun foo(array: Array<Int>) {
    (array[0] : <!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>) = 22
}