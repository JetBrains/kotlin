// WITH_RUNTIME
// IS_APPLICABLE: false

class MyClass {
    fun foo1() = Unit
    fun foo2() = Unit
    fun foo3() = Unit

    fun foo4(a: MyClass) {
        this.foo1()<caret>
        this.foo2()
        this.foo3()
    }
}