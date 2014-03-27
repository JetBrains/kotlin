// "Change 'foo' function return type to '([ERROR : NoSuchType]) -> Int'" "false"
// ACTION: Disable 'Make Types Implicit In Lambda (May Break Code)'
// ACTION: Edit intention settings
// ACTION: Make types implicit in lambda (may break code)
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.Int</td></tr><tr><td>Found:</td><td>([ERROR : NoSuchType]) &rarr; kotlin.Int</td></tr></table></html>
// ERROR: Unresolved reference: NoSuchType

fun foo(): Int {
    return { (x: NoSuchType<caret>) -> 42 }
}