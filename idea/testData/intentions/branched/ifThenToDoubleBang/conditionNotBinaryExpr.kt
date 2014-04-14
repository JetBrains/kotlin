// WITH_RUNTIME
// IS_APPLICABLE: false
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.Boolean</td></tr><tr><td>Found:</td><td>kotlin.Int</td></tr></table></html>
// ERROR: Condition must be of type kotlin.Boolean, but is of type kotlin.Int
// ERROR: Infix call corresponds to a dot-qualified call 'foo.times(10)' which is not allowed on a nullable receiver 'foo'. Use ?.-qualified call instead

fun String?.times(a: Int): Boolean = a == 0

fun main(args: Array<String>) {
    val foo: Int? = 4
    if (foo * 10<caret>) {
        foo
    }
    else {
        throw NullPointerException()
    }
}
