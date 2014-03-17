fun doSth(): String? {
    val x?: String = "abc"
    return x?.<caret>reverse()
}
fun main(args: Array<String>) {
    val y = doSth()
}
