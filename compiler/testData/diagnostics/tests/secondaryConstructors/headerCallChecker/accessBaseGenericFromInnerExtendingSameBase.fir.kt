// !DIAGNOSTICS: -UNUSED_PARAMETER

open class Base<T>(p: Any?) {
    fun foo1(t: T) {}
}

class D: Base<Int>("") {
    inner class B : Base<String> {
        constructor() : <!INAPPLICABLE_CANDIDATE!>super<!>(foo1(""))
        constructor(x: Int) : <!INAPPLICABLE_CANDIDATE!>super<!>(foo1(1))
    }
}
