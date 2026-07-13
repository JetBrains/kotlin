// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
package test

sealed class MyResult {
    class Ok(val value: String) : MyResult()
    class Err(val message: String) : MyResult()
}

fun handle(r: MyResult?) {
    if (r is <expr>MyResult.Ok</expr>?) {}
}
