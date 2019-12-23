// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, elvis-operator-expression -> paragraph 1 -> sentence 2
 * RELEVANT PLACES: expressions, elvis-operator-expression -> paragraph 1 -> sentence 1
 * expressions, elvis-operator-expression -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Check Elvis evaluation
 */


fun box(): String {
    val x : Boolean? = null ?: getNull() ?: A().b ?: getTrue() ?: false
    try {
        val y = null ?: throw  ExcA()
    } catch (e: ExcA) {

        if (x == true) return "OK"
    }

    return "NOK"
}
fun getTrue() = true

fun getNull(): Boolean? = null

class A(val b: Boolean? = null)

class ExcA() : Exception()