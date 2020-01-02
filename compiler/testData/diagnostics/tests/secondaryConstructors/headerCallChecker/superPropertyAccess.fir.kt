// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(val prop: Int)
class A : B {
    constructor(x: Int, y: Int = x + prop + this.prop + super.prop) :
        <!INAPPLICABLE_CANDIDATE!>super<!>(x + prop + this.prop + super.prop)
}
