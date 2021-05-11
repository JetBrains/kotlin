// FIR_COMPARISON
fun foo() {
    takeHandler1 {
        takeHandler2 { <caret> }
    }
}

fun takeHandler1(handler: () -> Unit){}
inline fun takeHandler2(handler: () -> Unit){}

// INVOCATION_COUNT: 1
// ABSENT: return
// EXIST: { lookupString: "return@takeHandler1", itemText: "return", tailText: "@takeHandler1", attributes: "bold" }
// EXIST: { lookupString: "return@takeHandler2", itemText: "return", tailText: "@takeHandler2", attributes: "bold" }
