fun some(list: List<String>): String {
    return list.<caret>
}

// EXIST: { lookupString: "[]", itemText: "[]", tailText: "(index: Int)", typeText: "String", attributes: "bold" }
