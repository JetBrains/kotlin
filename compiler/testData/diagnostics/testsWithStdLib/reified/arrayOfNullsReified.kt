// !DIAGNOSTICS: -NOTHING_TO_INLINE -UNUSED_VARIABLE

fun <T> foo() {
    val x = <!TYPE_PARAMETER_AS_REIFIED!>arrayOfNulls<!><T>(5)
}

inline fun <reified T> bar() {
    val x = arrayOfNulls<T>(5)
}

fun baz() {
    bar<Int>()
    val x: Array<Int?> = arrayOfNulls(5)
}
