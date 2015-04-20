// !DIAGNOSTICS:-UNUSED_VARIABLE

import kotlin.reflect.*

class A {
    fun foo() {}
}

fun A?.foo() {}

val f: KMemberFunction0<A, Unit> = A::foo
val g: KExtensionFunction0<A, Unit> = A?::foo
