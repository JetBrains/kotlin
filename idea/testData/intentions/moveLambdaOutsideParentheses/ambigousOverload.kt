// IS_AVAILABLE: true
// ERROR: None of the following functions can be called with the arguments supplied: <br>public fun bar(a: kotlin.Int = ..., f: (kotlin.Int) -> kotlin.Int): kotlin.Unit defined in root package<br>public fun bar(a: kotlin.Int, b: kotlin.Int, f: (kotlin.Int) -> kotlin.Int): kotlin.Unit defined in root package
// ERROR: Unresolved reference: it

fun foo() {
    bar(<caret>{ it })
}

fun bar(a: Int = 0, f: (Int) -> Int) { }
fun bar(a: Int, b: Int, f: (Int) -> Int) { }

