// RUN_PIPELINE_TILL: SOURCE
interface MutableMatrix<T> {
}

fun <T> toMutableMatrix(): MutableMatrix<T> {
    return <!INTERFACE_AS_FUNCTION!>MutableMatrix<!><T>()
}
