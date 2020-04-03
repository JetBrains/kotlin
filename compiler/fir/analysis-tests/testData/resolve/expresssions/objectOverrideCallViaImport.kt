import Derived.foo

interface Base {
    fun foo() {}
}

object Derived : Base

fun test() {
    // See KT-35730
    foo() // Derived.foo is more correct here
}