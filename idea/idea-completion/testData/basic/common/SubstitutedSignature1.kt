fun foo(list: MutableList<String>) {
    list.<caret>
}

// EXIST: { itemText: "add", tailText: "(e: String)", typeText: "Boolean" }
// EXIST: { itemText: "iterator", tailText: "()", typeText: "Iterator<String>" }
