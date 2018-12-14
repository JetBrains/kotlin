// WITH_RUNTIME

class MyClass {
    fun foo1() = Unit
    fun foo2() = Unit
    fun foo3() = Unit

    fun foo4(it: MyClass) {
        it.foo1()<caret>
        it.foo2()
        it.foo3()
    }
}