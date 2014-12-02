fun foo(list: List<String>) {
    list.<caret>
}
// EXIST: { itemText: "firstOrNull", tailText: "(predicate: (String) -> Boolean) for Iterable<String> in kotlin", typeText: "String?" }
// EXIST: { itemText: "toMap", tailText: "(selector: (String) -> K) for Iterable<String> in kotlin", typeText: "Map<K, String>" }
