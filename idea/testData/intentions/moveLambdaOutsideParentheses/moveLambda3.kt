// IS_APPLICABLE: true
// ERROR: Type mismatch: inferred type is () -> ??? but Int was expected
// ERROR: No value passed for parameter b
// ERROR: Unresolved reference: it
// SKIP_ERRORS_AFTER
fun foo() {
    bar({ it <caret>})
}

fun bar(a: Int, b: (Int) -> Int) {
    b(a)
}
