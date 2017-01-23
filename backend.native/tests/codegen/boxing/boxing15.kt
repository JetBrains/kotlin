fun main(args: Array<String>) {
    println(foo(17))
}

fun <T : Int> foo(x: T): Int = x