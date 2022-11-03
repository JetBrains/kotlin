// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL

abstract class TestType<V: Any> {
    open inner class Inner(val item: V)
}

class Derived: TestType<Long>() {
    inner class DerivedInner(item: Long): Inner(item)
}
