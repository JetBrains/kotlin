// FIR_IDENTICAL
// FIR_COMPARISON
inline fun run (p: () -> Unit) {}

fun foo() = run {
    <caret>
}

// INVOCATION_COUNT: 1
// EXIST: { lookupString: "return@run", itemText: "return", tailText: "@run", attributes: "bold" }
// ABSENT: return
