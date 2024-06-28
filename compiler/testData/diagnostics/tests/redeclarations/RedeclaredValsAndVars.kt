// FIR_DUMP
// ISSUE: KT-54405

class A {
    operator fun component1() = 1
    operator fun component2() = ""
}

fun testRedeclaration(b: Boolean) {
    val <!REDECLARATION!>y<!> = 1
    val <!NAME_SHADOWING, REDECLARATION!>y<!> = 2
    val <!REDECLARATION!>`_`<!> = 3
    val <!NAME_SHADOWING, REDECLARATION!>`_`<!> = 4
    {
        var <!REDECLARATION!>a<!> = 10
        var <!NAME_SHADOWING, REDECLARATION!>a<!> = 11
    }
}

fun testNoRedeclaration(list: List<Int>, b: Boolean) {
    for (el in list) {
        val <!NAME_SHADOWING!>el<!> = 42
    }
    if (b) {
        val z = 3
    } else {
        val z = 4
    }
    val (`_`, _) = A()
}
