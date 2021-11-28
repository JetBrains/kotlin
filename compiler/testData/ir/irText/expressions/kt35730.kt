// FIR_IDENTICAL
import Derived.foo
interface Base {
    fun foo() {}
}
object Derived : Base
fun test() {
    // Both calls resolved to Base.foo()
    foo()
    Derived.foo()
}