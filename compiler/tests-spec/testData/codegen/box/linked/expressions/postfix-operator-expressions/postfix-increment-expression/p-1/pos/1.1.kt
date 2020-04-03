// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: expressions, postfix-operator-expressions, postfix-increment-expression -> paragraph 1 -> sentence 1
 * RELEVANT PLACES: expressions, postfix-operator-expressions, postfix-increment-expression -> paragraph 5 -> sentence 1
 * overloadable-operators -> paragraph 4 -> sentence 1
 * statements, assignments -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check postfix increment expression
 */

fun box(): String {
    var a = A()
    val res: A = a++
    return if (a.i == 1 && res.i == 0) "OK"
    else "NOK"
}

class A(var i: Int = 0)  {
    operator fun inc(): A {
        return A(i+1)
    }
}