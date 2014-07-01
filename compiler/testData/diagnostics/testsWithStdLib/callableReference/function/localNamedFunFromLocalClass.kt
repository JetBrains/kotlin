import kotlin.reflect.*

fun main() {
    fun foo() {}
    fun bar(<!UNUSED_PARAMETER!>x<!>: Int) {}
    fun baz() = "OK"
    
    class A {
        val x = ::foo
        val y = ::bar
        val z = ::baz

        fun main() {
            x : KFunction0<Unit>
            y : KFunction1<Int, Unit>
            z : KFunction0<String>
        }
    }
}
