// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
open class Base<T>(p: Any?) {
    fun foo1(t: T) {}
}

fun Base<Int>.foo() {
    class B : Base<String> {
        constructor() : super(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>foo1<!>(""))
        constructor(x: Int) : super(foo1(1))
        constructor(x: Int, y: Int) : super(this@foo.foo1(12))
        constructor(x: Int, y: Int, z: Int) : super(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this@B<!>.foo1(""))
    }
}
