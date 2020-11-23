abstract class A<X : CharSequence> {
    inner class Inner
    fun foo(x: Inner.() -> Unit) {}
}

object B : A<String>() {

    fun bar() {
        val y: Inner.() -> Unit = {}
        foo(y)
        baz(y)
    }
}

fun baz(x: (A<String>.Inner) -> Unit) {}
