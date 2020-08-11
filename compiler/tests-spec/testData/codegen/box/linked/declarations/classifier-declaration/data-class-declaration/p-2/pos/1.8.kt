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
 * NUMBER: 8
 * DESCRIPTION: data class with default parameters at primary constructor
 */


fun box(): String {
    val x = A() as B
    val y: A = A(a = 1, String.Companion)

    return if (checkRuntimeTypeIsSame(x, y))
        checkEquality(x, y)
    else "NOK"

}

open class B(open val a: Int, open val b: Any) {
    override fun hashCode(): Int {
        return 555
    }
}

data class A(override val a: Int = 1, override val b: Any = String) : B(a, b)

fun <X : B, Y : B> checkEquality(x: X, y: Y): String {
    if (x.equals(y)) {
        if (x.a == y.a && x.b == y.b) {
            if (x.hashCode() == y.hashCode())
                return "OK"
            else return "hashCode are not equal"
        } else {
            println("a prop equals: " + (x.a == y.a))
            println("b prop equals: " + (x.b == y.b))
            return "props are not equal"
        }
    } else return "not equals"
}

fun <X : B, Y : B> checkRuntimeTypeIsSame(x: X, y: Y): Boolean = (x is A && y is A)



