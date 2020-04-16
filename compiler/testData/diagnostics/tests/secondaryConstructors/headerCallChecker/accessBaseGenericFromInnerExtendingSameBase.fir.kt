// !DIAGNOSTICS: -UNUSED_PARAMETER

open class Base<T>(p: Any?) {
    fun foo1(t: T) {}
}

class D: Base<Int>("") {
    inner class B : Base<String> {
        constructor() : super(<!INAPPLICABLE_CANDIDATE!>foo1<!>(""))
        constructor(x: Int) : super(foo1(1))
    }
}
