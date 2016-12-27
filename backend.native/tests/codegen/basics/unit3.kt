fun main(args: Array<String>) {
    foo(Unit)
}

fun foo(x: Any) {
    println(x.toString())
}