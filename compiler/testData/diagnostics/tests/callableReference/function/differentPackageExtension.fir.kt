// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// FILE: a.kt

package first

class A

fun A.foo() {}
fun A.bar() {}
fun A.baz() {}

// FILE: b.kt

package other

import kotlin.reflect.KFunction1

import first.A
import first.foo

fun main() {
    val x = first.A::foo
    first.A::bar
    A::baz

    checkSubtype<KFunction1<A, Unit>>(x)
}
