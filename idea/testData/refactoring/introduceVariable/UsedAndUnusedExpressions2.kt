class A {
    fun test() = 1
}

fun main(args: Array<String>) {
    val a = A()
    a.test()

    val b = <selection>a.test()</selection>
}
