fun main(args: Array<String>) {
    val foo: String? = "foo"
    if (true) {
        foo?.<caret>length
    }
}
