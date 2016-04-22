// WITH_RUNTIME
// IS_APPLICABLE: false
fun foo(list: List<MutableCollection<Int>>) {
    <caret>for (collection in list) {
        collection.add(collection.size)
    }
}