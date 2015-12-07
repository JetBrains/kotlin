fun test(p1: (String) -> Int, p2: (Int) -> Int, p3: (String) -> Char) {
    listOf("a", "b").map<caret>
}

// EXIST: { itemText: "map", tailText: " {...} (transform: (String) -> R) for Iterable<T> in kotlin.collections", typeText:"List<R>" }
// EXIST: { itemText: "map", tailText: "(p1) for Iterable<T> in kotlin.collections", typeText: "List<Int>" }
// ABSENT: { itemText: "map", tailText: "(p2) for Iterable<T> in kotlin.collections", typeText: "List<Int>" }
// EXIST: { itemText: "map", tailText: "(p3) for Iterable<T> in kotlin.collections", typeText: "List<Char>" }
