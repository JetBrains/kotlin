// FIR_COMPARISON
fun foo(): Boolean {
    ret<caret>
}

// INVOCATION_COUNT: 1
// WITH_ORDER
// EXIST: { lookupString: "return", itemText: "return", tailText: null, attributes: "bold" }
// EXIST: { lookupString: "return false", itemText: "return false", tailText: null, attributes: "bold" }
// EXIST: { lookupString: "return true", itemText: "return true", tailText: null, attributes: "bold" }
// NOTHING_ELSE
