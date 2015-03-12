// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(open val parentProperty: String)
class A : B {
    val myProp: String = ""
    override val parentProperty: String = ""
    constructor(arg: String = <!UNRESOLVED_REFERENCE!>myProp<!>): super(<!UNRESOLVED_REFERENCE!>myProp<!>) {}
    constructor(x1: String, arg: String = <!NO_THIS!>this<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>myProp<!>): super(<!NO_THIS!>this<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>myProp<!>) {}
    constructor(x1: String, x2: String, arg: String = <!UNRESOLVED_REFERENCE!>parentProperty<!>): super(<!UNRESOLVED_REFERENCE!>parentProperty<!>) {}
    constructor(x1: String, x2: String, x3: String, arg: String = <!SUPER_NOT_AVAILABLE!>super<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>parentProperty<!>): super(<!SUPER_NOT_AVAILABLE!>super<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>parentProperty<!>) {}
    constructor(x1: String, x2: String, x3: String, x4: String, arg: String = foo(<!NO_THIS!>this<!>)): super(foo(<!NO_THIS!>this<!>)) {}
    constructor(x1: String, x2: String, x3: String, x4: String, x5: String, arg: String = foo(this<!UNRESOLVED_REFERENCE!>@A<!>)): super(foo(this<!UNRESOLVED_REFERENCE!>@A<!>)) {}
}
fun foo(x: A) = ""
