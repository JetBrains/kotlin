// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 2
 * declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 3
 * declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 4
 * declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 5
 * NUMBER: 4
 * DESCRIPTION: data class with default parameters at primary constructor
 */

open class B(open val a: Int, open val b: Any)
data class A(override val a: Int = 1, override val b: String = "str") : B(a, b)

fun box(): String {
    val x = A(1, "str")
    val y = A()

    return if (checkRuntimeTypeIsSame(x, y))
        checkEquality(x, y)
    else "NOK"
}

fun <X : B, Y : B> checkRuntimeTypeIsSame(x: X, y: Y): Boolean = (x is A && y is A)

fun <X : B, Y : B> checkEquality(x: X, y: Y): String {
    if (x.equals(y)) {
        if (x.a == y.a && x.b == y.b) {
            if (x.hashCode() == y.hashCode())
                return "OK"
            else return "hashCode are not equal"
        } else return "props are not equal"
    } else return "not equals"
}


