fun foo(iterable: Iterable<String>) {
    iterable.singl<caret>
}

// WITH_ORDER
// EXIST: { itemText: "single", tailText: "() for Iterable<T> in kotlin.collections" }
// EXIST: { itemText: "single", tailText: " {...} (predicate: (String) -> Boolean) for Iterable<T> in kotlin.collections" }
// EXIST: { itemText: "singleOrNull", tailText: "() for Iterable<T> in kotlin.collections" }
// EXIST: { itemText: "singleOrNull", tailText: " {...} (predicate: (String) -> Boolean) for Iterable<T> in kotlin.collections" }
// NOTHING_ELSE
