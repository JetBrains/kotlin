// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 7
 * PRIMARY LINKS: expressions, not-null-assertion-expressions -> paragraph 1 -> sentence 1
 * expressions, not-null-assertion-expressions -> paragraph 1 -> sentence 2
 * type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 9
 * NUMBER: 2
 * DESCRIPTION: Type-checking is + not-null assertion expressions
 */

fun box(): String {

    val l: Any? = null
    try {
        if (l!! is List<*>?) {
            return "NOK"
        }
    } catch (e: java.lang.NullPointerException) {
        return "OK"
    }
    return "NOK"
}