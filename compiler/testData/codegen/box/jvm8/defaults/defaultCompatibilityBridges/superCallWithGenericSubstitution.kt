// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: JVM_IR
// MODULE: library
// JVM_DEFAULT_MODE: disable
// FILE: lib.kt
package base

interface A<TA, UA> {
    fun <Str> f(t: TA, u: UA): Str = "Fail" as Str
}

open class B<UB> : A<Int, UB>

// MODULE: main(library)
// JVM_DEFAULT_MODE: all-compatibility
// FILE: box.kt
import base.*

interface C<TC> : A<TC, Double> {
    override fun <Str> f(t: TC, u: Double): Str = "OK" as Str
}

open class D : B<Double>(), C<Int>

class E : D() {
    fun g(): String = super.f(1, 0.0)
}

fun box(): String = E().g()
