// "Change 'foo' function return type to '([ERROR : NoSuchType]) -> Int'" "false"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>jet.Int</td></tr><tr><td>Found:</td><td>([ERROR : NoSuchType]) &rarr; jet.Int</td></tr></table></html>
// ERROR: Unresolved reference: NoSuchType
fun foo(): Int {
    return { (x: NoSuchType<caret>) -> 42 }
}