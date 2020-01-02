// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(x: Int)
class A : B {
    val prop = 1
    constructor(x: Int, y: Int = x + prop + this.prop) :
        <!INAPPLICABLE_CANDIDATE!>super<!>(x + prop + this.prop)
}
