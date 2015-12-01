fun foo(list: List<String>) {
    list.<caret>
}
// EXIST: { itemText: "firstOrNull", tailText: " {...} (predicate: (String) -> Boolean) for Iterable<T> in kotlin", typeText: "String?" }
// EXIST: { itemText: "toMapBy", tailText: " {...} (selector: (String) -> K) for Iterable<T> in kotlin", typeText: "Map<K, String>" }
