data class A1(val <!REDECLARATION, REDECLARATION, REDECLARATION!>x<!>: Int, val y: String, val <!REDECLARATION, REDECLARATION, REDECLARATION!>x<!>: Int) {
    val z = ""
}

data class A2(val <!REDECLARATION!>x<!>: Int, val y: String) {
    val <!REDECLARATION!>x<!> = ""
}

data class A3(<!REDECLARATION, REDECLARATION, REDECLARATION!>val<!SYNTAX!><!> :Int<!>, <!REDECLARATION, REDECLARATION, REDECLARATION!>val<!SYNTAX!><!> : Int<!>)
