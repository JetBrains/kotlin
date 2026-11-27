// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, comparison-expressions -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: expressions, comparison-expressions -> paragraph 1 -> sentence 1
 * expressions, comparison-expressions -> paragraph 2 -> sentence 2
 * expressions, comparison-expressions -> paragraph 3 -> sentence 1
 * expressions, comparison-expressions -> paragraph 4 -> sentence 1
 * overloadable-operators -> paragraph 4 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: These operators are overloadable (A > B)
 */

//A > B is exactly the same as integerLess(0, A.compareTo(B))
fun box(): String {
    val a1 = A(-1)
    val a2 = A(-3)
    if (a1 > a2)
        if (a1.isCompared && !a2.isCompared)
            return "OK"
    return "NOK"
}

class A(val a: Int)  {
    var isCompared = false
    operator fun compareTo(other: A): Int = run {
        isCompared = true
        this.a - other.a
    }
}