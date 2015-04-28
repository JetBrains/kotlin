// IS_APPLICABLE: true
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.Int</td></tr><tr><td>Found:</td><td>() &rarr; ???</td></tr></table></html>
// ERROR: No value passed for parameter b
// ERROR: Unresolved reference: it
fun foo() {
    bar({ it <caret>})
}

fun bar(a: Int, b: (Int) -> Int) {
    b(a)
}
