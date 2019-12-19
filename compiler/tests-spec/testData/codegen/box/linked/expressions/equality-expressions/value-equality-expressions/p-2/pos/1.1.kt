// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, equality-expressions, value-equality-expressions -> paragraph 2 -> sentence 1
 * RELEVANT PLACES: expressions, equality-expressions, value-equality-expressions -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check value-equality-expression
 */


//A == B is exactly the same as (A as? Any)?.equals(B) ?: (B === null) where equals is the method of kotlin.Any;
fun box(): String {
    val x = A(false)
    val y = A(false)

    if (x == y) {
        if (x.isEqualsCalled && !y.isEqualsCalled)
            return "OK"
    }
    return "NOK"
}


data class A(val a: Boolean) {
    var isEqualsCalled = false

    override fun equals(anObject: Any?): Boolean {
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