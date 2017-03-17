inline fun <reified T> foo(i2: Any): T {
    return i2 as T
}

fun bar(i1: Int): Int {
    return foo<Int>(i1)
}

fun main(args: Array<String>) {
    println(bar(33))
}
