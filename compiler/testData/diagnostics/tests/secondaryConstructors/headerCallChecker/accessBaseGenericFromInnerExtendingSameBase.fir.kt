// !DIAGNOSTICS: -UNUSED_PARAMETER

open class Base<T>(p: Any?) {
    fun foo1(t: T) {}
}

class D: Base<Int>("") {
    inner class B : Base<String> {
        constructor() : super(foo1(<!ARGUMENT_TYPE_MISMATCH!>""<!>))
        constructor(x: Int) : super(foo1(1))
    }
}
