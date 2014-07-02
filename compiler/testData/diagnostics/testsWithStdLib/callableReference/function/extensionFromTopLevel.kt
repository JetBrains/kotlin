import kotlin.reflect.*

class A

fun A.foo() {}
fun A.bar(<!UNUSED_PARAMETER!>x<!>: Int) {}
fun A.baz() = "OK"

fun main() {
    val x = A::foo
    val y = A::bar
    val z = A::baz

    x : KExtensionFunction0<A, Unit>
    y : KExtensionFunction1<A, Int, Unit>
    z : KExtensionFunction0<A, String>
}
