// WITH_RUNTIME

class MyClass {
    fun foo1(a: MyClass): MyClass = this
    fun foo2(): MyClass = this
    fun foo3(): MyClass = this

    fun foo4() {
        val a = MyClass()
        a.foo1(this).foo2().foo3()
        a.foo2()<caret>
        a.foo3()
    }
}