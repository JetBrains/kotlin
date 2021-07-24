fun test(<!REDECLARATION, REDECLARATION!>a<!>: Int, <!REDECLARATION, REDECLARATION!>a<!>: String) {}

fun test2(block: (Int, String) -> Unit) { }

fun main() {
    test2 { <!REDECLARATION, REDECLARATION!>b<!>, <!REDECLARATION, REDECLARATION!>b<!> -> ; }
}
