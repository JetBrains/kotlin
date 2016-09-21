// IS_APPLICABLE: false
fun main(args: Array<String>) {
    val foo: String? = "foo"
    <caret>if (foo == null) {
        null
    }
    else {
        null
    }
}
