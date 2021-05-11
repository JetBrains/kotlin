// FIR_COMPARISON
fun foo(): Iterable<String> {
    ret<caret>
}

// INVOCATION_COUNT: 1
// WITH_ORDER
// EXIST: { lookupString: "return", itemText: "return", tailText: null, attributes: "bold" }
// EXIST: { lookupString: "return emptyList()", itemText: "return", tailText: " emptyList()", attributes: "bold" }
// NOTHING_ELSE
