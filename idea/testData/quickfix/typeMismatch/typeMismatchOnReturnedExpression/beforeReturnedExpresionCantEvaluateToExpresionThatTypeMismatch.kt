// "class org.jetbrains.kotlin.idea.quickfix.ChangeFunctionReturnTypeFix" "false"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.Int</td></tr><tr><td>Found:</td><td>kotlin.String</td></tr></table></html>
fun foo(): Int = if (true) ""<caret>: Int else 4
