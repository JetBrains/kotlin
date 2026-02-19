// FILE: main.kt
import dependency.A

fun test() {
    val b = A.B(<expr>dependency.Foo()</expr>)
}
// FILE: dependency.kt
package dependency

class Foo

sealed class A {
    class B(foo: Foo): A()
}
