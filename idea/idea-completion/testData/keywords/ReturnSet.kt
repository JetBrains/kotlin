// FIR_COMPARISON
fun<T> foo(): Set<T> {
    ret<caret>
}

// INVOCATION_COUNT: 1
// WITH_ORDER
// EXIST: { lookupString: "return", itemText: "return", tailText: null, attributes: "bold" }
// EXIST: { lookupString: "return emptySet()", itemText: "return", tailText: " emptySet()", attributes: "bold" }
// NOTHING_ELSE
