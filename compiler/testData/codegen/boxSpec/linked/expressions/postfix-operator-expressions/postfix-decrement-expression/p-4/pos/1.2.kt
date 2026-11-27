// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, postfix-operator-expressions, postfix-decrement-expression -> paragraph 4 -> sentence 1
 * PRIMARY LINKS: statements, assignments -> paragraph 3 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: check for A-- expression A must be an assignable expression (a navigation expression referring to a mutable property)
 */


fun box(): String {
    var b = B()
    val x = b.a--
    return if (x.i == 0 && b.a.i == -1)
        "OK"
    else "NOK"
}

class A(var i :Int = 0) {
    operator fun dec(): A {
        return A(i-1)
    }
}
class B() {
    var a: A = A()
}