// "Change 'foo' function return type to '([ERROR : NoSuchType]) -> Int'" "false"
// ACTION: Create annotation 'NoSuchType'
// ACTION: Create class 'NoSuchType'
// ACTION: Create enum 'NoSuchType'
// ACTION: Create interface 'NoSuchType'
// ACTION: Remove explicit lambda parameter types (may break code)
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.Int</td></tr><tr><td>Found:</td><td>([ERROR : NoSuchType]) &rarr; kotlin.Int</td></tr></table></html>
// ERROR: Unresolved reference: NoSuchType

fun foo(): Int {
    return { (x: NoSuchType<caret>) -> 42 }
}