// FIR_IDENTICAL
// FIR_COMPARISON
fun foo() {
    `fun` {
        `val`({ ret<caret> })
    }
}

inline fun `fun`(handler: () -> Unit){}
inline fun `val`(handler: () -> Unit){}

// INVOCATION_COUNT: 1
// EXIST: { lookupString: "return", itemText: "return", tailText: null, attributes: "bold" }
// EXIST: { lookupString: "return@`fun`", itemText: "return", tailText: "@`fun`", attributes: "bold" }
// EXIST: { lookupString: "return@`val`", itemText: "return", tailText: "@`val`", attributes: "bold" }
// NOTHING_ELSE
