// IS_APPLICABLE: false
// ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type kotlin.String?
fun main(args: Array<String>) {
    val foo: String? = "foo"
    if<caret> {
        foo.length
    } else null
}
