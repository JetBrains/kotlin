// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    fun foo() = 1
    constructor(x: Int)
    constructor(x: Int, y: Int, z: Int = x <!AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>foo<!>() + this.<!UNRESOLVED_REFERENCE!>foo<!>()) :
        this(x + foo() + this.foo())
}
