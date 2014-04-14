// WITH_RUNTIME
// IS_APPLICABLE: false
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.Boolean</td></tr><tr><td>Found:</td><td>() &rarr; kotlin.String?</td></tr></table></html>
// ERROR: Condition must be of type kotlin.Boolean, but is of type () -> kotlin.String?
fun main(args: Array<String>) {
    val foo: String? = "foo"
    if<caret> {
        foo
    } else throw NullPointerException()
}
