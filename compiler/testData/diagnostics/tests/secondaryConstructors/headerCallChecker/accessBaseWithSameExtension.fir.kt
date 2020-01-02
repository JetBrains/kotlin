// !DIAGNOSTICS: -UNUSED_PARAMETER
open class Base(p: Any?) {
    fun foo1() {}
}

fun Base.foo() {
    class B : Base {
        constructor() : <!INAPPLICABLE_CANDIDATE!>super<!>(foo1())
        constructor(x: Int) : <!INAPPLICABLE_CANDIDATE!>super<!>(this@foo.foo1())
        constructor(x: Int, y: Int) : <!INAPPLICABLE_CANDIDATE!>super<!>(this@B.foo1())
    }
}
