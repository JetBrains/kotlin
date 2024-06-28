// ISSUE: KT-58902

open class A {
    open fun foo(x: String, y: String? = null): String = x + (y ?: "K")
}

fun box(): String {
    return run {
        class MyClass : A() {
            override fun foo(x: String, y: String?) = super.foo(x, y)
        }

        MyClass()
    }.foo("O")
}