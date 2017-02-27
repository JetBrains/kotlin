fun main(a: Array<String>) {
    val foo = mutableListOf("x", "a", "b")
    foo.sort()
    println(foo.toString())

    var bar = mutableListOf(239, 42, -1, 100500, 0)
    bar.sort()
    println(bar.toString())
}