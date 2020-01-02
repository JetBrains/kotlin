// !DIAGNOSTICS: -UNUSED_PARAMETER
open class Base<T>(p: Any?) {
    fun foo1(t: T) {}
}

fun Base<Int>.foo() {
    class B : Base<String> {
        constructor() : <!INAPPLICABLE_CANDIDATE!>super<!>(foo1(""))
        constructor(x: Int) : <!INAPPLICABLE_CANDIDATE!>super<!>(foo1(1))
        constructor(x: Int, y: Int) : <!INAPPLICABLE_CANDIDATE!>super<!>(this@foo.foo1(12))
        constructor(x: Int, y: Int, z: Int) : <!INAPPLICABLE_CANDIDATE!>super<!>(this@B.foo1(""))
    }
}
