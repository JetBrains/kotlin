import server.*;

class Client {
    public fun foo() {
        val a = A<String>()
        a.foo = "a"
        println("a.foo = ${a.foo}")

        val b = B()
        b.foo = "b"
        println("b.foo = ${b.foo}")
    }
}
