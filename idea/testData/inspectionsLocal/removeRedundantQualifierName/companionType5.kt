package my.simple.name

class F {
    class CheckClass

    fun foo(a: F<caret>.CheckClass) {}

    companion object {
        class CheckClass
    }
}
