// PROBLEM: none
package my.simple.name

class F {
    class CheckClass

    fun foo(a: Companion<caret>.CheckClass) {}

    companion object {
        class CheckClass
    }
}
