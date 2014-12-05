fun foo(list: List<String>): Collection<Int> {
    return list.<caret>
}

// EXIST: { lookupString: "map", tailText: "(transform: (String) -> Int) for Iterable<T> in kotlin", typeText: "List<Int>" }
// ABSENT: filter
