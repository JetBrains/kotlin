// WITH_RUNTIME
fun test(collection: Collection<Int>): Collection<Int> {
    return if (collection.isNotEmpty<caret>()) {
        collection
    } else {
        listOf(1)
    }
}