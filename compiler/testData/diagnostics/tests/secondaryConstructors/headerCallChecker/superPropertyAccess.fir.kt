// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(val prop: Int)
class A : B {
    constructor(x: Int, y: Int = x + prop + this.prop + super.prop) :
        <!INAPPLICABLE_CANDIDATE!>super<!>(x <!AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>prop<!> + this.<!UNRESOLVED_REFERENCE!>prop<!> + super.prop)
}
