// !CHECK_TYPE
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

    checkSubtype<KFunction0<Unit>>(x)
    checkSubtype<KFunction1<Int, Unit>>(y)
    checkSubtype<KFunction0<String>>(z)
}
