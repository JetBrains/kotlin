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
