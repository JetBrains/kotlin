// FIR_COMPARISON
fun test() {}
fun some() {
    val localVal = 1
    test(<caret>)
}

// Test no exceptions are thrown
// INVOCATION_COUNT: 1
// EXIST: localVal