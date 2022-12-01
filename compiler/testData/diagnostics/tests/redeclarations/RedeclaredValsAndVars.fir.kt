// ISSUE: KT-54405

class A {
    operator fun component1() = 1
    operator fun component2() = ""
}

fun test(b: Boolean) {
    val <!REDECLARATION!>y<!> = 1
    val <!REDECLARATION!>y<!> = 2
    val <!REDECLARATION!>`_`<!> = 3
    val <!REDECLARATION!>`_`<!> = 4
    {
        var <!REDECLARATION!>a<!> = 10
        var <!REDECLARATION!>a<!> = 11
    }
    if (b) {
        val z = 3
        val <!REDECLARATION!>x<!> = 5
        val <!REDECLARATION!>x<!> = 6
    } else {
        val z = 4
    }
}

fun test2() {
    val (`_`, _) = A()
}