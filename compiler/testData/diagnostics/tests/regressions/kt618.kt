// FIR_IDENTICAL
package lol

class B() {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun plusAssign(other : B) : String {
        return "s"
    }
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun minusAssign(other : B) : String {
        return "s"
    }
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun remAssign(other : B) : String {
        return "s"
    }
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun divAssign(other : B) : String {
        return "s"
    }
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun timesAssign(other : B) : String {
        return "s"
    }
}

fun main() {
    var c = B()
    c <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT!>+=<!> B()
    c <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT!>*=<!> B()
    c <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT!>/=<!> B()
    c <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT!>-=<!> B()
    c <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT!>%=<!> B()
}
