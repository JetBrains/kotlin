// TARGET_BACKEND: JVM
// MODULE: library
// JVM_DEFAULT_MODE: disable
// FILE: a.kt
package base

interface A<TA, UA> {
    fun <Str> f(t: TA, u: UA): Str = "Fail" as Str
}

open class B<UB> : A<Int, UB>

// MODULE: main(library)
// JVM_DEFAULT_MODE: enable
// FILE: source.kt
import base.*

interface C<TC> : A<TC, Double> {
    override fun <Str> f(t: TC, u: Double): Str = "OK" as Str
}

class D : B<Double>(), C<Int>

fun box(): String = D().f(1, 0.0)
