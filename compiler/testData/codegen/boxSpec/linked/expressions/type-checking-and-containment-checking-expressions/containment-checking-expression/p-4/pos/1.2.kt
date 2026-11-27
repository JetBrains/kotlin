// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, type-checking-and-containment-checking-expressions, containment-checking-expression -> paragraph 4 -> sentence 1
 * PRIMARY LINKS: expressions, type-checking-and-containment-checking-expressions, containment-checking-expression -> paragraph 1 -> sentence 2
 * expressions, type-checking-and-containment-checking-expressions, containment-checking-expression -> paragraph 1 -> sentence 1
 * expressions, type-checking-and-containment-checking-expressions, containment-checking-expression -> paragraph 3 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION:  A in B is exactly the same as B.contains(A);
 */

class A(val a: Set<Any>) {
    var isEvaluated: Boolean = false
    var isChecked = false
    operator fun contains(other: Any): Boolean = run {
        isChecked = true
        this.a.contains(other)
    }

    fun foo(): A {
        this.isEvaluated = true
        return this
    }
}

fun throwException(b: Boolean): Boolean {
    if (b) throw Exception()
    else return false
}

fun box(): String {
    val b = A(mutableSetOf(1, 3, false, 2, "azaza"))
    try {
       val k = (throwException(true) !in b.foo())
    } catch (e: Exception) {
        if (!b.isChecked && b.isEvaluated)
            return "OK"
    }
    return "NOK"
}