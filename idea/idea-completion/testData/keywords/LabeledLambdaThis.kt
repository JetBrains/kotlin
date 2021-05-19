// FIR_IDENTICAL
// FIR_COMPARISON
fun foo(): String.() -> Unit {
    return (label@ {
        f {
            <caret>
        }
    })
}

fun f(p: Any.() -> Unit){}

// INVOCATION_COUNT: 1
// EXIST: { lookupString: "this", itemText: "this", tailText: null, typeText: "Any", attributes: "bold" }
// ABSENT: "this@f"
// EXIST: { lookupString: "this@label", itemText: "this", tailText: "@label", typeText: "String", attributes: "bold" }
