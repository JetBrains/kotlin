// "Change parameter 'z' type of function 'foo' to '(Int) -> String'" "false"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.Int</td></tr><tr><td>Found:</td><td>kotlin.String</td></tr></table></html>

// ACTION: Disable 'Move lambda function into parentheses'
// ACTION: Edit intention settings
// ACTION: Move lambda function into parentheses

fun foo(y: Int = 0, z: (Int) -> String = {""}) {
    foo {
        ""<caret>: Int
        ""
    }
}