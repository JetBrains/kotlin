// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, equality-expressions, value-equality-expressions -> paragraph 2 -> sentence 1
 * RELEVANT PLACES: expressions, equality-expressions, value-equality-expressions -> paragraph 3 -> sentence 1
 * NUMBER: 5
 * DESCRIPTION: check value-equality-expression
 */


//A == B is exactly the same as (A as? Any)?.equals(B) ?: (B === null) where equals is the method of kotlin.Any;
fun box(): String {
    val x: A = A(false)
    val y: Any = ""

    if ((x == y) == checkEqualls(x, y)) {
        if (x.isEqualsCalled)
            return "OK"
    }
    return "NOK"
}

fun checkEqualls(A: Any?, B: Any?): Boolean {
    return (A as? Any)?.equals(B) ?: (B === null)
}

data class A(val a: Boolean) {
    var isEqualsCalled = false

    override operator fun equals(anObject: Any?): Boolean {
        isEqualsCalled = true
        if (this === anObject) {
            return true
        }
        if (anObject is A) {
            if (anObject.a == a)
                return true
        }
        return false
    }
}