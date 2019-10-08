// WITH_RUNTIME

class MyClass {
    fun foo(c: Int) {
        val a = 23
        a.dec()
        a.dec()<caret>
        a.dec() + a
    }
}