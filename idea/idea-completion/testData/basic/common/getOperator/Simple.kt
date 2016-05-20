fun some(list: List<String>) {
    list.<caret>
}

// EXIST: { lookupString: "[]", itemText: "[]", tailText: "(index: Int)", typeText: "String", attributes: "bold" }
