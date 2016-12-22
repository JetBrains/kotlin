fun is42(x: Any?) {
    println(x == 42)
    println(42 == x)
}

fun main(args: Array<String>) {
    is42(16)
    is42(42)
    is42("42")
}