// IS_AVAILABLE: true
// ERROR: None of the following functions can be called with the arguments supplied: <br>public fun bar(a: Int = ..., f: (Int) -> Int): Unit defined in root package in file ambigousOverload.kt<br>public fun bar(a: Int, b: Int, f: (Int) -> Int): Unit defined in root package in file ambigousOverload.kt
// ERROR: Unresolved reference: it
// SKIP_ERRORS_AFTER

fun foo() {
    bar(<caret>{ it })
}

fun bar(a: Int = 0, f: (Int) -> Int) { }
fun bar(a: Int, b: Int, f: (Int) -> Int) { }

