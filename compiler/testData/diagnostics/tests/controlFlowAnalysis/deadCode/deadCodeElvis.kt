// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

val nullableInt: Int? get() = null

fun test1(): Int {
    return nullableInt?.let { return it } ?: 0
}

fun test2(): Int {
    val it = nullableInt
    return if (it != null) {
        return it
    } else {
        0
    }
}
