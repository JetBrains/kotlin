fun test(<!REDECLARATION!>a<!>: Int, <!REDECLARATION!>a<!>: String) {}

fun test2(block: (Int, String) -> Unit) { }

fun main() {
    test2 { <!REDECLARATION!>b<!>, <!REDECLARATION!>b<!> -> ; }
}
