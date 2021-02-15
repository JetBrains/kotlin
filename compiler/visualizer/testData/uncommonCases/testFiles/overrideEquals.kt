class A(val a: Int) {
    override fun equals(other: Any?): Boolean {
        if (other !is A) return false
        return this.a == other.a
    }
}

open class B(val b: Int) {
    override fun equals(other: Any?): Boolean {
        if (other !is B) return false
        return this.b == other.b
    }
}

class C(c: Int): B(c) {}

val areEqual = A(10) == A(11)
val areEqual2 = C(10) == C(11)
val areEqual3 = A(10) == C(11)