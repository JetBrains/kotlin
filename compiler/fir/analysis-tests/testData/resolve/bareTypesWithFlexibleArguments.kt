// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LATEST_LV_DIFFERENCE
// FULL_JDK

fun <T> Collection<T>?.concat(collection: Collection<T>): Collection<T>? {
    if (this is LinkedHashSet) {
        addAll(<!ARGUMENT_TYPE_MISMATCH!>collection<!>)
        return this
    }
    return this
}
