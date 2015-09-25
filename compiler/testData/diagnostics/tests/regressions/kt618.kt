package lol

class B() {
    operator fun plusAssign(<!UNUSED_PARAMETER!>other<!> : B) : String {
        return "s"
    }
    operator fun minusAssign(<!UNUSED_PARAMETER!>other<!> : B) : String {
        return "s"
    }
    operator fun modAssign(<!UNUSED_PARAMETER!>other<!> : B) : String {
        return "s"
    }
    operator fun divAssign(<!UNUSED_PARAMETER!>other<!> : B) : String {
        return "s"
    }
    operator fun timesAssign(<!UNUSED_PARAMETER!>other<!> : B) : String {
        return "s"
    }
}

fun main(args : Array<String>) {
    var c = B()
    c <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT!>+=<!> B()
    c <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT!>*=<!> B()
    c <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT!>/=<!> B()
    c <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT!>-=<!> B()
    c <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT!>%=<!> B()
}
