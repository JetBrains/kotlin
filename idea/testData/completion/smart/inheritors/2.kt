fun foo(): List<String> {
    return <caret>
}

// EXIST: { lookupString: "ArrayList", itemText: "ArrayList<String>", tailText: "(...) (java.util)" }
