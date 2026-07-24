// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
package test

sealed class MyResult {
    class Ok(val value: String) : MyResult()
    class Err(val message: String) : MyResult()
}

// The qualified type `MyResult.Ok` appears as a property type, NOT inside an `is`/`as` operator.
// CSR for types only applies to `FirTypeOperatorCall` (see KT-84719), so the shortener
// must NOT collapse the whole qualifier to a simple name here.
val x: <expr>MyResult.Ok</expr>? = null
