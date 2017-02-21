fun main(args: Array<String>) {
    foo()
    foo()
}

fun foo() {
    val array = arrayOf("a", "b")
    println(array[0])
    array[0] = "42"
}