interface A {
    fun foo(foo: Any? = null): Boolean
}

interface B {
    fun foo(foo: Any? = null): Boolean
}

open class B2 : B {
    override fun foo(foo: Any?): Boolean {
        return false
    }
}

open class C : B2(), A {
    final override fun foo(foo: Any?): Boolean {
        return true
    }
}

fun box(): String {
    val a: A = C()
    return if (a.foo()) "OK" else "fail"
}