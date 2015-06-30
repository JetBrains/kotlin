fun <caret>f(p: Int) = p + p

fun complexFun(): Int {
}

fun main(args: Array<String>) {
    f(complexFun())
}