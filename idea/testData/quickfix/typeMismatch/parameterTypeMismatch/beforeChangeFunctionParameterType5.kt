// "class org.jetbrains.jet.plugin.quickfix.ChangeParameterTypeFix" "false"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>jet.Int</td></tr><tr><td>Found:</td><td>jet.String</td></tr></table></html>
fun foo(y: Int = 0, z: (Int) -> String = {""}) {
    foo(""<caret>: Int)
}