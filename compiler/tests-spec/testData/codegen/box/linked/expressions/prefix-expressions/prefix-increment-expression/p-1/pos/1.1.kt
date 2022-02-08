// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, prefix-expressions, prefix-increment-expression -> paragraph 1 -> sentence 1
 * PRIMARY LINKS: expressions, prefix-expressions, prefix-increment-expression -> paragraph 5 -> sentence 1
 * overloadable-operators -> paragraph 4 -> sentence 1
 * statements, assignments -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION:
 */

fun box(): String {
    var a = A()
    val res: A = ++a
    return if (a.i == 1) "OK"
    else "NOK"
}

class A() {
    var i = 0

    operator fun inc(): A {
        this.i++
        return this
    }

}