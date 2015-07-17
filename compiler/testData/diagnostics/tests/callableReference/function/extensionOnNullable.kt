// !DIAGNOSTICS:-UNUSED_VARIABLE

import kotlin.reflect.*

class A {
    fun foo() {}
}

fun A?.foo() {}

val f: KFunction1<A, Unit> = A::foo
val g: KFunction1<A, Unit> = A?::foo
