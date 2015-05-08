// "class org.jetbrains.kotlin.idea.quickfix.ChangeParameterTypeFix" "false"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.Int</td></tr><tr><td>Found:</td><td>kotlin.String</td></tr></table></html>
fun foo(y: Int = 0, z: (Int) -> String = {""}) {
    foo(""<caret>: Int)
}
