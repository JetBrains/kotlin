// "Cast expression 'x' to 'String'" "false"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.String</td></tr><tr><td>Found:</td><td>kotlin.Int</td></tr></table></html>
fun foo(x: Int) {
    x<caret>: String
}