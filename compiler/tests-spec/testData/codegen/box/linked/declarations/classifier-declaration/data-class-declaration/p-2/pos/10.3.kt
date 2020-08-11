// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 10
 * SECONDARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 7
 * NUMBER: 3
 * DESCRIPTION: copy with defaults
 */
const val TMP_HASH_CODE = 555;

open class B(open val a: Int, open val b: Any) {
    override fun hashCode(): Int {
        return TMP_HASH_CODE
    }
}

data class A(override val a: Int, override val b: String) : B(a, b)

fun box(): String {
    val x = A(1, "str")
    val y = x.copy()

    if (y.a == x.a && y.b == x.b) {
        return "OK"
    } else return "nok"
}

