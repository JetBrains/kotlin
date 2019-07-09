// WITH_RUNTIME

class MyClass {
    fun foo1() = Unit
    fun foo2(a: MyClass) = Unit
    fun foo3() = Unit

    fun foo4(a: MyClass) {
        val a = MyClass()
        a.foo1()
        a.foo3()<caret>
        a.foo2(this)
        a.foo3()
    }
}