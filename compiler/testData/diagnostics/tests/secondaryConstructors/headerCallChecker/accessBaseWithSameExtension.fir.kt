// !DIAGNOSTICS: -UNUSED_PARAMETER
open class Base(p: Any?) {
    fun foo1() {}
}

fun Base.foo() {
    class B : Base {
        constructor() : super(foo1())
        constructor(x: Int) : super(this@foo.foo1())
        constructor(x: Int, y: Int) : super(<!UNRESOLVED_LABEL!>this@B<!>.<!UNRESOLVED_REFERENCE!>foo1<!>())
    }
}
