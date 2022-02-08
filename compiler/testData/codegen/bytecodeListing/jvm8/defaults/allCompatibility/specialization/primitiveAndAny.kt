// !JVM_DEFAULT_MODE: all-compatibility
// JVM_TARGET: 1.8
// WITH_STDLIB

interface Base {
    fun test(): Int? = 0
}

interface Derived: Base {
    override fun test(): Int = 1
}

interface Mixed: Base, Derived

open class A: Base, Derived
open class B: Mixed