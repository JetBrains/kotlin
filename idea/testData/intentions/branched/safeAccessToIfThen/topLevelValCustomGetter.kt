val a: String?
    get() = ""

fun main(args: Array<String>) {
    println(a?.<caret>length)
}
