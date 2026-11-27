// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-220
 * MAIN LINK: expressions, not-null-assertion-expression -> paragraph 2 -> sentence 2
 * PRIMARY LINKS: expressions, not-null-assertion-expression -> paragraph 2 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: For an expression e!!, if the type of e is nullable, a not-null assertion expression checks, whether the evaluation result of e is equal to null and, if it is, throws a runtime exception.
 */


fun box(): String {
    val x = A(A(A(A())))
    if ( x.a!!.a!!.a!!.a == null)
    {
        try {
           val t = x.a!!.a!!.a!!.a!!
        }catch (e: java.lang.NullPointerException){
            return "OK"
        }
    }
    return "NOK"
}

class A ( val a: A? = null)