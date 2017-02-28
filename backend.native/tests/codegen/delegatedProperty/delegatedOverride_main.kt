import a.*

open class C: B() {
    override val x: Int = 156

    fun foo() {
        println(x)

        println(super<B>.x)
        bar()
    }
}

fun main(args: Array<String>) {
    val c = C()
    c.foo()
}
