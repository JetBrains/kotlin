// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// FILE: main.kt
package usage

fun handle(r: pkg.MyResult) {
    if (r is <expr>pkg.MyResult</expr>.Ok) {}
}

// FILE: dependency.kt
package pkg

sealed class MyResult {
    class Ok(val value: String) : MyResult()
    class Err(val message: String) : MyResult()
}
