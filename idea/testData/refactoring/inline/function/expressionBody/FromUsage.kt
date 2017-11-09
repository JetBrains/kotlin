fun f(p1: Int, p2: Int) = p1 + p2

fun main(args: Array<String>) {
    println(f(1, 2))
    println(<caret>f(3, 4))
    println(f(5, 6))
}