// FILE: a.kt

package first

fun foo() {}
fun bar(<!UNUSED_PARAMETER!>x<!>: Int) {}
fun baz() = "OK"

// FILE: b.kt

package other

import kotlin.reflect.*

import first.foo
import first.bar
import first.baz

fun main() {
    val x = ::foo
    val y = ::bar
    val z = ::baz

    x : KFunction0<Unit>
    y : KFunction1<Int, Unit>
    z : KFunction0<String>
}
