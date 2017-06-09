enum class A(one: Int, val two: Int = one) {
    FOO(42)
}

fun main(args: Array<String>) {
    println(A.FOO.two)
}