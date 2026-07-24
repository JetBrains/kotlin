// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
package test

sealed class MyResult {
    class Ok(val value: String) : MyResult()
    class Err(val message: String) : MyResult()
}

fun handle(r: MyResult) {
    when (r) {
        is <expr>MyResult.Ok</expr> -> {}
        is MyResult.Err -> {}
    }
}
