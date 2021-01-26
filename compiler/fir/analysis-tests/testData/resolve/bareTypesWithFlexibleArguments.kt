// WITH_STDLIB
// FULL_JDK

fun <T> Collection<T>?.concat(collection: Collection<T>): Collection<T>? {
    if (this is LinkedHashSet) {
        addAll(collection)
        return this
    }
    return this
}
