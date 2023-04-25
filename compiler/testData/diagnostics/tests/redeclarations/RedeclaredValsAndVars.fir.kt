// FIR_DUMP
// ISSUE: KT-54405

class A {
    operator fun component1() = 1
    operator fun component2() = ""
}

fun testRedeclaration(b: Boolean) {
    val <!REDECLARATION!>y<!> = 1
    val <!REDECLARATION!>y<!> = 2
    val <!REDECLARATION!>`_`<!> = 3
    val <!REDECLARATION!>`_`<!> = 4
    {
        var <!REDECLARATION!>a<!> = 10
        var <!REDECLARATION!>a<!> = 11
    }
}

fun testNoRedeclaration(list: List<Int>, b: Boolean) {
    for (el in list) {
        val el = 42
    }
    if (b) {
        val z = 3
    } else {
        val z = 4
    }
    val (`_`, _) = A()
}