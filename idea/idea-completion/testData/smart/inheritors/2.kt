fun foo(): List<String> {
    return <caret>
}

// EXIST: { lookupString: "ArrayList", itemText: "ArrayList", tailText: "(...) (java.util)" }
