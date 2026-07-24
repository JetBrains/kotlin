// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
package test

sealed class MyResult {
    class Ok(val value: String) : MyResult()
    class Err(val message: String) : MyResult()
}

// Top-level class with the same simple name as a sealed subclass — CSR must not apply.
class Ok

fun handle(r: MyResult) {
    if (r is <expr>MyResult.Ok</expr>) {}
}
