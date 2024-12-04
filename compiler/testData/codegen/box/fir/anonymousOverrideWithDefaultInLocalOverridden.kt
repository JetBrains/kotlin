// ISSUE: KT-58902

fun box(): String {
    open class Outer {
        open inner class A {
            open fun foo(x: String, y: String? = null): String = x + (y ?: "K")
        }
    }

    val b = object : Outer() {
        inner class MyClass : A() {
            override fun foo(x: String, y: String?) = super.foo(x, y)
        }
    }

    return b.MyClass().foo("O")
}