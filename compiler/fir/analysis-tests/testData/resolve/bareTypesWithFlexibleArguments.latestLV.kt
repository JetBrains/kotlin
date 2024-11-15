// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LATEST_LV_DIFFERENCE
// FULL_JDK

fun <T> Collection<T>?.concat(collection: Collection<T>): Collection<T>? {
    if (this is <!CANNOT_CHECK_FOR_ERASED!>LinkedHashSet<!>) {
        addAll(collection)
        return this
    }
    return this
}
