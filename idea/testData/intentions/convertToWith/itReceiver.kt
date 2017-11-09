// WITH_RUNTIME
// IS_APPLICABLE: false

class MyClass {
    fun foo1() = Unit
    fun foo2() = Unit
    fun foo3() = Unit

    fun foo4(a: MyClass) {
        a.let {
            it.foo1()<caret>
            it.foo2()
            it.foo3()
        }
    }
}