// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * PLACE: expressions, postfix-operator-expressions, postfix-decrement-expression -> paragraph 6 -> sentence 1
 * RELEVANT PLACES: expressions, postfix-operator-expressions, postfix-decrement-expression -> paragraph 5 -> sentence 1
 * overloadable-operators -> paragraph 4 -> sentence 1
 * statements, assignments -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check postfix decrement expression has the same type as its operand expression
 */

fun box(): String {
    var a = A()
    val res: Any? = a--

    return if (res is A ) "OK"
    else "NOK"
}


open class A(var i: Int = 0) {
    open operator fun dec(): A {
        return A(i - 1)
    }
}