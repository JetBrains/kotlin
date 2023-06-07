// ISSUE: KT-58902

open class Outer {
    open inner class A {
        open fun foo(x: String, y: String? = null): String = x + (y ?: "K")
    }
}

fun box(): String {
    val b = object : Outer() {
        inner class MyClass : A() {
            override fun foo(x: String, y: String?) = super.foo(x, y)
        }
    }

    return b.MyClass().foo("O")
}