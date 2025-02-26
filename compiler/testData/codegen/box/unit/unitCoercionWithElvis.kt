// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// ^^^ Compiler v2.0.0:error: 'when' expression must be exhaustive. Add the 'false' branch or an 'else' branch.
// WITH_STDLIB
// ISSUE: KT-71751

fun launch(x: () -> Unit) {
    x()
}

fun box(): String {
    var result: String = "fail"
    val job = launch {
        "test".let {
            null
        } ?: run { // this is not called if it is the last thing in the block
            result = "OK"
        }
    }

    return result
}
