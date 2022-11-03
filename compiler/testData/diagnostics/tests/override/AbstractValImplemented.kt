// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
abstract class A {
    abstract val i: Int
}

class B() : A() {
    override val i = 1
}
