interface MutableMatrix<T> {
}

fun <T> toMutableMatrix(): MutableMatrix<T> {
    return <!UNRESOLVED_REFERENCE!>MutableMatrix<!><T>()
}