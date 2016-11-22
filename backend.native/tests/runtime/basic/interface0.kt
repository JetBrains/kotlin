
interface A {
    fun b() = c()
    fun c()
}

class B(): A {
    override fun c() {
        println("PASSED")
    }
}

fun main(args: Array<String>) {
    val a:A = B()
    a.b()
}

