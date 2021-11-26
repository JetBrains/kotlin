// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, range-expression -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: expressions, range-expression -> paragraph 1 -> sentence 1
 * expressions, range-expression -> paragraph 1 -> sentence 2
 * expressions, range-expression -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: A..B is exactly the same as A.rangeTo(B)
 */

class A(val a: Int) {
    var isRangeToCalled = false
    operator fun rangeTo(o: A): MutableList<Int> {
        isRangeToCalled = true
        val x: MutableList<Int> = mutableListOf<Int>()
        for (i in a..o.a)
            x.add(i)
        return x
    }
}

fun box(): String {
    val a1 = A(0)
    val a2 = A(5)
    val x = a1..a2
    if (a1.isRangeToCalled && x is MutableList<Int>)
        return "OK"
    return "NOK"
}