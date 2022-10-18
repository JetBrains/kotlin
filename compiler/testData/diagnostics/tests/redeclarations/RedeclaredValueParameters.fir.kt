fun test(<!REDECLARATION, REDECLARATION!>a<!>: Int, <!REDECLARATION, REDECLARATION!>a<!>: String) {}

fun test2(block: (Int, String) -> Unit) { }

fun main() {
    test2 { <!REDECLARATION!>b<!>, <!REDECLARATION!>b<!> -> ; }

    val func: (Int, Int) -> Int = fun(_, _): Int { return 42 }
}
