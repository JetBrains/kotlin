// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(val prop: Int)
class A : B {
    constructor(x: Int, y: Int = x <!AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>prop<!> + this.<!UNRESOLVED_REFERENCE!>prop<!> + super.<!UNRESOLVED_REFERENCE!>prop<!>) :
        super(x + prop + this.prop + super.prop)
}
