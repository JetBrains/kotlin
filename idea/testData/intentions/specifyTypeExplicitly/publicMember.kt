// IS_APPLICABLE: false
// WITH_RUNTIME
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.Unit</td></tr><tr><td>Found:</td><td>kotlin.String</td></tr></table></html>

class A {
    public fun <caret>foo() {
        return ""
    }
}
