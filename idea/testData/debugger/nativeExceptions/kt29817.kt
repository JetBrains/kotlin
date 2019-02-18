package sample

fun hello(): String = "Hello, Kotlin/Native!"

fun bar(count: Int) {
    foo(count - 1)
}

fun foo(count: Int) {
    if (count <= 0) throw Exception("foo")
    bar(count - 1)
}

fun main() {
    foo(20)
    println(hello())
}