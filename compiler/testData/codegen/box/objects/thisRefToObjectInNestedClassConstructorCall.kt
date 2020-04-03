open class Base(val s: String)

object Host {
    class Derived : Base(this.foo())

    fun foo() = "OK"
}

fun box() = Host.Derived().s