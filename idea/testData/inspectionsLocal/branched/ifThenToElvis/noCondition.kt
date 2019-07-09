// PROBLEM: none
fun main(args: Array<String>) {
    val foo: String? = "foo"
    val bar = "bar"
    if<caret> {
        foo
    } else bar
}
