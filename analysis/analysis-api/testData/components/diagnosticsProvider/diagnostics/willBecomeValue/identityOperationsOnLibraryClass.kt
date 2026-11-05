// WITH_STDLIB
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
@file:OptIn(ExperimentalValueClassesApi::class)

package lib

@WillBecomeValue
class Money(val cents: Long) {
    override fun equals(other: Any?): Boolean = other is Money && cents == other.cents
    override fun hashCode(): Int = cents.hashCode()
    override fun toString(): String = "Money($cents)"
}

// MODULE: main(lib)
// FILE: main.kt
import lib.Money

fun check(a: Money, b: Money?) {
    a === b
    a !== b
    a == b
    b === null
    synchronized(a) {}
    System.identityHashCode(a)
}

fun <T : Money> checkTypeParameter(a: T, b: T) {
    a === b
}
