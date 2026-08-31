class A(val x: String) {
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean = x == other.x
}

class B(val x: String) {
    override fun equals(other: Any?): Boolean = other is B && x == other.x
}

interface Base {
    val x: String
}

class C(override val x: String) : Base {
    override fun equals(@EqualityBound(C::class) other: Any?): Boolean = x == other.x
}

interface NewBase {
    val x: String
}

open class WithNewBase(override val x: String) : Base

open class AliasTarget(val x: String)
open class ChangedAliasTarget(val x: String)

typealias RemovedAlias = AliasTarget
typealias ChangedAlias = AliasTarget
