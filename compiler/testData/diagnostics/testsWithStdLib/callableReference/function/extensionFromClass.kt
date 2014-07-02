import kotlin.reflect.*

class A {
    fun main() {
        val x = ::foo
        val y = ::bar
        val z = ::baz

        x : KExtensionFunction0<A, Unit>
        y : KExtensionFunction1<A, Int, Unit>
        z : KExtensionFunction0<A, String>
    }
}

fun A.foo() {}
fun A.bar(<!UNUSED_PARAMETER!>x<!>: Int) {}
fun A.baz() = "OK"
