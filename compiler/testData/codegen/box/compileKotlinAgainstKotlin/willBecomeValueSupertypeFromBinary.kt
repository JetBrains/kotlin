// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalValueClassesApi
// MODULE: lib
// FILE: lib.kt

package lib

// A '@WillBecomeValue' class may only extend a class which is going to lose its identity along with it,
// so compiling 'Sub' requires the annotation to be loaded from the binary dependency.
@WillBecomeValue
abstract class LibBase

// MODULE: main(lib)
// FILE: main.kt

import lib.LibBase

@WillBecomeValue
class Sub(val x: Int) : LibBase() {
    override fun equals(other: Any?): Boolean = other is Sub && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Sub($x)"
}

fun box(): String = if (Sub(1) == Sub(1)) "OK" else "Fail"
