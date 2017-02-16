abstract class A<in T> {
    abstract fun foo(x: T)
}

class B : A<Int>() {
    override fun foo(x: Int) {
        println("B: $x")
    }
}

class C : A<Any>() {
    override fun foo(x: Any) {
        println("C: $x")
    }
}

fun foo(arg: A<Int>) {
    arg.foo(42)
}

fun main(args: Array<String>) {
    foo(B())
    foo(C())
}