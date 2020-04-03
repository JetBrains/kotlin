// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: expressions, prefix-expressions, prefix-decrement-expression -> paragraph 6 -> sentence 1
 * RELEVANT PLACES: expressions, prefix-expressions, prefix-decrement-expression -> paragraph 5 -> sentence 1
 * overloadable-operators -> paragraph 4 -> sentence 1
 * statements, assignments -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION:
 */

fun box(): String {
    var a = A()
    val res: Any? = --a

    return if (res is A) "OK"
    else "NOK"
}

class A() {
    var i = 0

    operator fun dec(): A {
        this.i--
        return this
    }

}