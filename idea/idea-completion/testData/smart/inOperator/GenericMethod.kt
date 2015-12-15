fun foo(s: String) {
    if (s in <caret>)
}

// EXIST: { lookupString:"listOf", itemText: "listOf", tailText: "(vararg elements: T) (kotlin.collections)", typeText:"List<T>" }
