// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
class A(t : Int) : Comparable<A> {
    var i = t
    override fun compareTo(other : A) = (this.i - other.i)
}
