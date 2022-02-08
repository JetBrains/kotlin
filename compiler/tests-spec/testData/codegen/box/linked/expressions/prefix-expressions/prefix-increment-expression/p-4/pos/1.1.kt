// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, prefix-expressions, prefix-increment-expression -> paragraph 4 -> sentence 1
 * PRIMARY LINKS: statements, assignments -> paragraph 3 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: check for a prefix increment expression ++A expression A must be an assignable expression (an indexing expression)
 */


fun box(): String {
    val arr = arrayOf(A(), A(), A())

    val a = ++arr[0]
    val b = (++arr[2]).i

    return if (arr[0].i == 1 && a.i == 1 && arr[1].i == 0 && arr[2].i == 1 && b == 1)
        "OK"
    else "NOK"
}


class A() {
    var i = 0

    operator fun inc(): A {
        this.i++
        return this
    }

}