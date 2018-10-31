package lol

class B() {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun plusAssign(<!UNUSED_PARAMETER!>other<!> : B) : String {
        return "s"
    }
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun minusAssign(<!UNUSED_PARAMETER!>other<!> : B) : String {
        return "s"
    }
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun remAssign(<!UNUSED_PARAMETER!>other<!> : B) : String {
        return "s"
    }
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun divAssign(<!UNUSED_PARAMETER!>other<!> : B) : String {
        return "s"
    }
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun timesAssign(<!UNUSED_PARAMETER!>other<!> : B) : String {
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