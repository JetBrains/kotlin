// FILE: a.kt

package first

class A {
    fun foo() {}
    fun bar(<!UNUSED_PARAMETER!>x<!>: Int) {}
    fun baz() = "OK"
}

// FILE: b.kt

package other

import kotlin.reflect.*

import first.A

fun main() {
    val x = first.A::foo
    val y = first.A::bar
    val z = A::baz

    x : KMemberFunction0<A, Unit>
    y : KMemberFunction1<A, Int, Unit>
    z : KMemberFunction0<A, String>
}
