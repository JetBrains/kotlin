// FULL_JDK

fun test(collection: MutableCollection<Boolean>) {
    collection.removeIf { it }
}