// !DIAGNOSTICS: -UNUSED_EXPRESSION
// FILE: a.kt

package first

class A

fun A.foo() {}
fun A.bar() {}
fun A.baz() {}

// FILE: b.kt

package other

import kotlin.reflect.KExtensionFunction0

import first.A
import first.foo

fun main() {
    val x = first.A::foo
    first.A::<!UNRESOLVED_REFERENCE!>bar<!>
    A::<!UNRESOLVED_REFERENCE!>baz<!>

    x : KExtensionFunction0<A, Unit>
}