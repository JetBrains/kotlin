// !DIAGNOSTICS: -UNUSED_PARAMETER

open class Base<T>(p: Any?) {
    fun foo1(t: T) {}
}

class D: Base<Int>(1) {
    inner class B : Base<Int> {
        constructor() : <!INAPPLICABLE_CANDIDATE!>super<!>(foo1(1))
        constructor(x: Int) : <!INAPPLICABLE_CANDIDATE!>super<!>(this@B.foo1(1))
        constructor(x: Int, y: Int) : <!INAPPLICABLE_CANDIDATE!>super<!>(this@D.foo1(1))
    }
}
