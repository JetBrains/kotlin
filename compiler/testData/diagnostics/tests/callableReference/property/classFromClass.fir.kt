// !DIAGNOSTICS:-UNUSED_VARIABLE

import kotlin.reflect.*

class A(var g: A) {
    val f: Int = 0

    fun test() {
        val fRef: KProperty1<A, Int> = A::f
        val gRef: KMutableProperty1<A, A> = A::g
    }
}
