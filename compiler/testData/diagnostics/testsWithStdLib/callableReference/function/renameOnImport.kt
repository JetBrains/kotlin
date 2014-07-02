// FILE: a.kt

package other

fun foo() {}

class A {
    fun bar() = 42
}

fun A.baz(<!UNUSED_PARAMETER!>x<!>: String) {}

// FILE: b.kt

import kotlin.reflect.*

import other.foo as foofoo
import other.A as AA
import other.baz as bazbaz

fun main() {
    val x = ::foofoo
    val y = AA::bar
    val z = AA::bazbaz

    x : KFunction0<Unit>
    y : KMemberFunction0<AA, Int>
    z : KExtensionFunction1<AA, String, Unit>
}
