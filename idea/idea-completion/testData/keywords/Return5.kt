// FIR_COMPARISON
fun foo() {
    takeHandler label@ {
        <caret>
    }
}

inline fun takeHandler(handler: () -> Unit){}

// INVOCATION_COUNT: 1
// EXIST: { lookupString: "return", itemText: "return", tailText: null, attributes: "bold" }
// ABSENT: "return@takeHandler"
// EXIST: { lookupString: "return@label", itemText: "return", tailText: "@label", attributes: "bold" }
