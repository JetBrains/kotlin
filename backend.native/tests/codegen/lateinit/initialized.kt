class A {
    lateinit var s: String

    fun foo() = s
}

fun main(args: Array<String>) {
    val a = A()
    a.s = "zzz"
    println(a.foo())
}