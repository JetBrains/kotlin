// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: expressions, postfix-operator-expressions, postfix-decrement-expression -> paragraph 4 -> sentence 1
 * RELEVANT PLACES: statements, assignments -> paragraph 3 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: check for a  decrement expression A-- expression A must be an assignable expression (an indexing expression)
 */

fun box(): String {
    val arr = arrayOf(A(), A(), A())

    val a = arr[0]--
    val b = (arr[2]--).i

    return if (arr[0].i == -1 && a.i == 0 && arr[1].i == 0 && arr[2].i == -1 && b == 0)
        "OK"
    else "NOK"
}

class A(var i :Int = 0) {
    operator fun dec(): A {
        return A(i-1)
    }
}