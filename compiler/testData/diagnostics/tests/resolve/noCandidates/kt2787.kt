// RUN_PIPELINE_TILL: FRONTEND
interface MutableMatrix<T> {
}

fun <T> toMutableMatrix(): MutableMatrix<T> {
    return <!RESOLUTION_TO_CLASSIFIER!>MutableMatrix<!><T>()
}
