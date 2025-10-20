// IGNORE_DEXING
// IGNORE_BACKEND: NATIVE
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE

fun `test unquoted string variable in }} json body`() {
    {}
}

fun `test unquoted string variable in {{ json body`() {
    {}
}

fun `test @`(): () -> String {
    return { "O" }
}

fun `test #`(): () -> String {
    return { "K" }
}

fun box(): String {
    return `test @`()() + `test #`()()
}