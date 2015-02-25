fun foo(s: String) {
    if (s in <caret>)
}

// EXIST: { lookupString:"listOf", itemText: "listOf", tailText: "(vararg values: T) (kotlin)", typeText:"List<T>" }
