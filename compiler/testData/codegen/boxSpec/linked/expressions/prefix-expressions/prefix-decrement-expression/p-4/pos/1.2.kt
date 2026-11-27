// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, prefix-expressions, prefix-decrement-expression -> paragraph 4 -> sentence 1
 * PRIMARY LINKS: statements, assignments -> paragraph 3 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: check for a prefix decrement expression --A expression A must be an assignable expression (a navigation expression referring to a mutable property)
 */


fun box(): String {
    var b = B()
    --b.a
    return if (b.a.i == -1)
        "OK"
    else "NOK"
}


class A() {
    var i = 0

    operator fun dec(): A {
        this.i--
        return this
    }

}

class B() {
    var a: A = A()
}