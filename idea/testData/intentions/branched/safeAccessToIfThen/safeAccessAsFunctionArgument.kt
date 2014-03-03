fun f(s: Int?) {
    println(s)
}

fun main(args: Array<String>) {
    val x: String? = "foo"
    f(x?.<caret>length)
}
