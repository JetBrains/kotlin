fun f(s: Int?) {
}

fun main(args: Array<String>) {
    val x: String? = "foo"
    f(x?.<caret>length)
}
