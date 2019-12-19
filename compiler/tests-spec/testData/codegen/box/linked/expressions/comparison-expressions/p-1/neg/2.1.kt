// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, comparison-expressions -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: expressions, comparison-expressions -> paragraph 1 -> sentence 2
 * EXCEPTION: compiletime
 */
class A(val a: Int)  {
    var isCompared = false
    fun compareTo(other: A): Int = run {
        isCompared = true
        this.a - other.a
    }
}

fun box() {
    val a3 = A(-1)
    val a4 = A(-3)

    val x = (a3 > a4)
}

