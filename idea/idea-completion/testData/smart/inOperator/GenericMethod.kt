fun foo(s: String) {
    if (s in <caret>)
}

// EXIST: { lookupString:"listOf", itemText: "listOf", tailText: "(vararg elements: T) (kotlin)", typeText:"List<T>" }
