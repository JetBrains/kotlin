// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, elvis-operator-expression -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: expressions, elvis-operator-expression -> paragraph 1 -> sentence 1
 * expressions, elvis-operator-expression -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Check Elvis evaluation
 */


fun box(): String {
    val x: Boolean? = null ?: getNull(null) ?: A().b ?: getTrue() ?: false
    val s = null == getNull(null) ?: !getNullableTrue()!! || getFalse() ?: false

    val k = ((getNull(null)?: getNull(null) ) ?: getNull(true)) ?: getFalse()
    try {
        val y = null ?: throw ExcA()
    } catch (e: ExcA) {

        if ((x == true && !s && k!!)) return "OK"
    }

    return "NOK"
}
fun getTrue() = true

fun getNull(b: Boolean?): Boolean? = b

class A(val b: Boolean? = null)

class ExcA() : Exception()

fun getFalse(): Boolean? { return false }

fun getNullableTrue(): Boolean? { return true }