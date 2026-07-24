// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
package test

sealed class MyResult {
    class Ok(val value: String) : MyResult()
    class Err(val message: String) : MyResult()
}

fun handle(r: MyResult) {
    val x = r as? <expr>MyResult.Ok</expr>
}
