class A {
    lateinit var s: String

    fun foo() = s
}

fun main(args: Array<String>) {
    val a = A()
    try {
        println(a.foo())
    }
    catch (e: RuntimeException) {
        println("OK")
        return
    }
    println("Fail")
}