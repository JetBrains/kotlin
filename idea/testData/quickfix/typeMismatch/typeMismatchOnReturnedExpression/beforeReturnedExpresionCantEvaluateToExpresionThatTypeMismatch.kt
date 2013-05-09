// "Change function 'foo' return type to 'String'" "false"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>jet.Int</td></tr><tr><td>Found:</td><td>jet.String</td></tr></table></html>
// ACTION: Disable 'Replace 'if' with 'when''
// ACTION: Edit intention settings
// ACTION: Replace 'if' with 'when'
fun foo(): Int = if (true) ""<caret>: Int else 4