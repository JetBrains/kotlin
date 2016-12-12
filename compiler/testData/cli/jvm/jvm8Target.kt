interface A {
    fun test() = "OK"
}

fun main(args: Array<String>) {
    println(object : A {}.test())
}