// JVM_DEFAULT_MODE: disable
// ENABLE_INTERFACE_BRIDGES

interface I1<T: Any> {
    var property: T
    fun foo(p: T)
    fun bar(): Any
}

// no bridges expected as there are no real overrides
interface I2<T: Number> : I1<T>

// bridges are expected to be generated as default interface methods
// JVM_DEFAULT_MODE does not affect bridges generation
interface I3<T: Number> : I2<T> {
    abstract override var property: T
    override fun foo(p: T)
    override fun bar(): Number
}

// no bridges expected as there are no real overrides
open abstract class C1<T: Int> : I3<T>

// bridges are expected both for Number & Object signatures
open abstract class C2<T: Int> : C1<T>() {
    abstract override var property: T
    abstract override fun foo(p: T)
    abstract override fun bar(): Int
}

open class C3<T: Int> : C2<T>() {
    @Suppress("UNCHECKED_CAST")
    override var property: T = 0 as T

    override fun foo(p: T) {}
    override fun bar(): Int = 42
}