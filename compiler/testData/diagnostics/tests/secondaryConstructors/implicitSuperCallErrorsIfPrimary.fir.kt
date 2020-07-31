// !DIAGNOSTICS: -UNUSED_PARAMETER
open class A(p1: String)

class B() : A("") {
    <!INAPPLICABLE_CANDIDATE!>constructor(s: String)<!> {
    }
}
