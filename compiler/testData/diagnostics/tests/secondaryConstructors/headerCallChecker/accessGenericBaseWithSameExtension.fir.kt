// !DIAGNOSTICS: -UNUSED_PARAMETER
open class Base<T>(p: Any?) {
    fun foo1(t: T) {}
}

fun Base<Int>.foo() {
    class B : Base<String> {
        constructor() : super(foo1(<!ARGUMENT_TYPE_MISMATCH!>""<!>))
        constructor(x: Int) : super(foo1(1))
        constructor(x: Int, y: Int) : super(this@foo.foo1(12))
        constructor(x: Int, y: Int, z: Int) : super(<!UNRESOLVED_LABEL!>this@B<!>.<!UNRESOLVED_REFERENCE!>foo1<!>(""))
    }
}
