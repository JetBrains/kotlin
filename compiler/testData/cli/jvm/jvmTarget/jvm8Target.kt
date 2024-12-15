interface A {
    fun test() = "OK"
}

fun main() {
    println(object : A {}.test())
}