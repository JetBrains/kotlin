// !DIAGNOSTICS: -UNUSED_PARAMETER
open class Base(p: Any?) {
    fun foo1() {}
}

fun Base.foo() {
    class B : Base {
        constructor() : super(foo1())
        constructor(x: Int) : super(this@foo.foo1())
        constructor(x: Int, y: Int) : super(this<!UNRESOLVED_LABEL!>@B<!>.<!UNRESOLVED_REFERENCE!>foo1<!>())
    }
}
