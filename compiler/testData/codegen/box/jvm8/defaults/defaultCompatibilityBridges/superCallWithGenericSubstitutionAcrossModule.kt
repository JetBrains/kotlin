// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: JVM_IR
// MODULE: lib1
// JVM_DEFAULT_MODE: disable
// FILE: lib1.kt
package lib1

interface A<TA, UA> {
    fun <Str> f(t: TA, u: UA): Str = "Fail" as Str
}

open class B<UB> : A<Int, UB>

// MODULE: lib2(lib1)
// JVM_DEFAULT_MODE: all-compatibility
// FILE: lib2.kt
package lib2

import lib1.*

interface C<TC> : A<TC, Double> {
    override fun <Str> f(t: TC, u: Double): Str = "OK" as Str
}

open class D : B<Double>(), C<Int>

// MODULE: main(lib1, lib2)
// JVM_DEFAULT_MODE: all
// FILE: box.kt
import lib2.*

class E : D() {
    fun g(): String = super.f(1, 0.0)
}

fun box(): String = E().g()
