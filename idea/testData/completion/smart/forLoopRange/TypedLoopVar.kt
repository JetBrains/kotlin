fun foo(p1: Collection<String>, p2: Collection<Int>, p3: Collection<String?>) {
    for (i: Any in <caret>)
}

// EXIST: p1
// EXIST: p2
// ABSENT: p3
// EXIST: { lookupString:"listOf", itemText: "listOf", tailText: "(vararg values: T) (kotlin)", typeText:"List<T>" }
