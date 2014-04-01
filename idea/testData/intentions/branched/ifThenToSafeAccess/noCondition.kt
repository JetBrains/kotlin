// IS_APPLICABLE: false
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.Boolean</td></tr><tr><td>Found:</td><td>() &rarr; kotlin.Int</td></tr></table></html>
// ERROR: Condition must be of type kotlin.Boolean, but is of type () -> kotlin.Int
// ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type kotlin.String?
fun main(args: Array<String>) {
    val foo: String? = "foo"
    if<caret> {
        foo.length
    } else null
}
