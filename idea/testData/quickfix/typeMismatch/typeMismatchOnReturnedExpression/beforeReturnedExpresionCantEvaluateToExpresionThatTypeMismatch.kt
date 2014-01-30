// "class org.jetbrains.jet.plugin.quickfix.ChangeFunctionReturnTypeFix" "false"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>jet.Int</td></tr><tr><td>Found:</td><td>jet.String</td></tr></table></html>
fun foo(): Int = if (true) ""<caret>: Int else 4