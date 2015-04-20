// !DIAGNOSTICS:-UNUSED_VARIABLE

import kotlin.reflect.*

class A(var g: A) {
    val f: Int = 0

    fun test() {
        val fRef: KMemberProperty<A, Int> = ::f
        val gRef: KMutableMemberProperty<A, A> = ::g
    }
}
