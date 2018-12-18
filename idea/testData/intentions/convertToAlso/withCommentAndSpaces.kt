// WITH_RUNTIME

class MyClass {
    fun foo1() = Unit
    fun foo2() = Unit
    fun foo3() = Unit

    fun foo4(a: MyClass) {
        // top comment
        val a = MyClass()
        // here is comment
        a.foo1()<caret>
        // comment

        // bbb
        a.foo2()
        a.foo3()
        // last comment won't be in
    }
}