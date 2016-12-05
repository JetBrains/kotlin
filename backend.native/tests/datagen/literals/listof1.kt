fun main(args: Array<String>) {
    val list = foo()
    println(list === foo())
    println(list.toString())
}

fun foo(): List<String> {
    return listOf("a", "b", "c")
}