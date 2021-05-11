// FIR_COMPARISON
fun foo() {
    takeHandler1 {
        takeHandler2 { <caret> }
    }
}

fun takeHandler1(handler: () -> Unit){}
fun takeHandler2(handler: () -> Unit){}

// INVOCATION_COUNT: 1
// ABSENT: return
// ABSENT: return@takeHandler1
// EXIST: { lookupString: "return@takeHandler2", itemText: "return", tailText: "@takeHandler2", attributes: "bold" }
