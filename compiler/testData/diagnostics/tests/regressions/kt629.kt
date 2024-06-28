// FIR_IDENTICAL
//KT-629 Assignments are parsed as expressions.

package kt629

class A() {
    var p = "yeah"
    operator fun rem(other : A) : A {
        return A();
    }
}

fun box() : Boolean {
    var c = A()
    val d = c;
    c %= A();
    return (c != d) && (<!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>c.p = "yeah"<!>)
}


fun box2() : Boolean {
    var c = A()
    return (<!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>c.p = "yeah"<!>) && true
}
