fun <caret>f(p: Int) = g()

fun g() {
}

fun complexFun(): Int {
}

fun main(args: Array<String>) {
    f(complexFun())
}