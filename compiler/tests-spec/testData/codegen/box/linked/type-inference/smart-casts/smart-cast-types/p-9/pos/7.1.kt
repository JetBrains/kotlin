// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 7
 * PRIMARY LINKS: expressions, not-null-assertion-expressions -> paragraph 1 -> sentence 1
 * expressions, not-null-assertion-expressions -> paragraph 1 -> sentence 3
 * type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 9
 * NUMBER: 1
 * DESCRIPTION: Type-checking is + not-null assertion expressions
 */

fun box(): String {

    val l: Any? = emptyList<String>()
    if (l is List<*>?) {
        l // List<*>? (smart cast from Any?)
        try {
            if (l!!.equals(emptyList<String>())) { // l!! is List<*>
                return "OK"
            }
        } catch (e: java.lang.NullPointerException) {
            return "NOK"
        }
    }
    return "NOK"
}