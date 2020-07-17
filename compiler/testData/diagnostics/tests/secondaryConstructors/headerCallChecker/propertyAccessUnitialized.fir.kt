// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(x: Int)
class A : B {
    val prop = 1
    constructor(x: Int, y: Int = x <!AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>prop<!> + this.<!UNRESOLVED_REFERENCE!>prop<!>) :
        super(x + prop + this.prop)
}
