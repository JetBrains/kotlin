package lol

class B() {
    fun plusAssign(<!UNUSED_PARAMETER!>other<!> : B) : String {
        return "s"
    }
    fun minusAssign(<!UNUSED_PARAMETER!>other<!> : B) : String {
        return "s"
    }
    fun modAssign(<!UNUSED_PARAMETER!>other<!> : B) : String {
        return "s"
    }
    fun divAssign(<!UNUSED_PARAMETER!>other<!> : B) : String {
        return "s"
    }
    fun timesAssign(<!UNUSED_PARAMETER!>other<!> : B) : String {
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
