// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

open class Base<T>(p: Any?) {
    fun foo1(t: T) {}
}

class D: Base<Int>(1) {
    inner class B : Base<Int> {
        constructor() : super(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>foo1<!>(1))
        constructor(x: Int) : super(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this@B<!>.foo1(1))
        constructor(x: Int, y: Int) : super(this@D.foo1(1))
    }
}
