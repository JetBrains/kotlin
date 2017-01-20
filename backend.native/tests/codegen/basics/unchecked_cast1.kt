fun main(args: Array<String>) {
    foo<String>("17")
    bar<String>("17")
    foo<String>(42)
    bar<String>(42)
}

fun <T> foo(x: Any?) {
    val y = x as T
    println(y.toString())
}

fun <T> bar(x: Any?) {
    val y = x as? T
    println(y.toString())
}