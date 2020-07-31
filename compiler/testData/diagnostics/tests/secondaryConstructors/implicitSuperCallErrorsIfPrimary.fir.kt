// !DIAGNOSTICS: -UNUSED_PARAMETER
open class A(p1: String)

class B() : A("") {
    <!INAPPLICABLE_CANDIDATE, PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED!>constructor(s: String)<!> {
    }
}
