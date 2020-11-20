data class A1(<!REDECLARATION!>val x: Int<!>, val y: String, <!REDECLARATION!>val x: Int<!>) {
    val z = ""
}

data class A2(<!REDECLARATION!>val x: Int<!>, val y: String) {
    <!REDECLARATION!>val x = ""<!>
}

data class A3(<!REDECLARATION!>val<!SYNTAX!><!> :Int<!>, <!REDECLARATION!>val<!SYNTAX!><!> : Int<!>)
