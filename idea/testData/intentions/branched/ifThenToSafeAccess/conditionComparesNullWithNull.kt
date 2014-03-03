// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo = "foo"
    if (null == <caret>null) {
        foo.length
    }
    else null
}
