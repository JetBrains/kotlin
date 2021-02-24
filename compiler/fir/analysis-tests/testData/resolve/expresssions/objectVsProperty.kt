<!REDECLARATION!>object A<!>

<!REDECLARATION!>val A = 10<!>


fun foo() = A

fun bar() {
    val A = ""
    val b = A
}

