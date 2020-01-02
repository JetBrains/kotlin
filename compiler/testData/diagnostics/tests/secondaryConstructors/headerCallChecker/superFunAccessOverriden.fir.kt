// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(x: Int) {
    open fun foo() = 1
}
class A : B {
    override fun foo() = 2
    constructor(x: Int, y: Int = x + foo() + this.foo() + super.foo()) :
        <!INAPPLICABLE_CANDIDATE!>super<!>(x + foo() + this.foo() + super.foo())
}
