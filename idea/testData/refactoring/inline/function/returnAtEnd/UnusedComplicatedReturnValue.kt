fun <caret>f(p: Int): Boolean {
    println(p)
    return g(p)
}

fun g(p: Int): Boolean = TODO()

fun main(args: Array<String>) {
    f(1)
}