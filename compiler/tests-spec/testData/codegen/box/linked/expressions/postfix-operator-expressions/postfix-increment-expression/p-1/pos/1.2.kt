// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, postfix-operator-expressions, postfix-increment-expression -> paragraph 1 -> sentence 1
 * PRIMARY LINKS: expressions, postfix-operator-expressions, postfix-increment-expression -> paragraph 5 -> sentence 1
 * overloadable-operators -> paragraph 4 -> sentence 1
 * statements, assignments -> paragraph 3 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: check postfix increment expression
 */


fun box(): String {
    var a1 = A()
    val res1: A = ++a1

    var a2 = A()
    val res2: A = a2++

    return if (res1.i == 1 && res2.i == 0) "OK"
    else "NOK"
}

class A(var i :Int = 0) {
    operator fun inc(): A {
        return A(i+1)
    }
}

