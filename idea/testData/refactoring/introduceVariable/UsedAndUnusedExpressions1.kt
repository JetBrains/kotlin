class A {
    fun test() = 1
}

fun main(args: Array<String>) {
    val a = A()
    <selection>a.test()</selection>

    val b = a.test()
}
