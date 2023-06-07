// ISSUE: KT-58902

fun box(): String {
    return run {
        open class A {
            open fun foo(x: String, y: String? = null): String = x + (y ?: "K")
        }

        class MyClass : A() {
            override fun foo(x: String, y: String?) = super.foo(x, y)
        }

        MyClass()
    }.foo("O")
}