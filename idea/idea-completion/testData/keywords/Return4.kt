// FIR_IDENTICAL
// FIR_COMPARISON
fun foo() {
    takeHandler label@ {
        <caret>
    }
}

fun takeHandler(handler: () -> Unit){}

// INVOCATION_COUNT: 1
// ABSENT: return
// ABSENT: "return@takeHandler"
// EXIST: { lookupString: "return@label", itemText: "return", tailText: "@label", attributes: "bold" }
