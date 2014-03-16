// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo: String? = "foo"
    if (null == <caret>null) {
        foo
    }
    else {
        null
    }
}
