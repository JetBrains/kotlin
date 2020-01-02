// !LANGUAGE: +NewInference

abstract class TestType<V: Any> {
    open inner class Inner(val item: V)
}

class Derived: TestType<Long>() {
    inner class DerivedInner(item: Long): <!INAPPLICABLE_CANDIDATE!>Inner<!>(item)
}
