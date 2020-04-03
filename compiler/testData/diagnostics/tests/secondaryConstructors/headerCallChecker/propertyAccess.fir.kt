// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    val prop = 1
    constructor(x: Int)
    constructor(x: Int, y: Int, z: Int = x + prop + this.prop) :
        <!INAPPLICABLE_CANDIDATE!>this<!>(x <!AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>prop<!> + this.<!UNRESOLVED_REFERENCE!>prop<!>)
}
