// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, logical-conjunction-expression -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: expressions, logical-conjunction-expression -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: operator && does not evaluate the right hand side argument unless the left hand side argument evaluated to false.
 */

fun box(): String {

    val aval = A()
    val x = aval.a(true) &&
            aval.b(true) &&
            aval.c(false) &&
            aval.d(false)

    if (aval.a && aval.b && aval.c && !aval.d && !x)
        return "OK"
    return "NOK"
}


class A (var a: Boolean = false,
         var b: Boolean = false,
         var c: Boolean = false,
         var d: Boolean = false){

    fun a(a: Boolean): Boolean { this.a = true; return a }

    fun b(a: Boolean): Boolean { this.b = true; return a }

    fun c(a: Boolean): Boolean { this.c = true; return a }

    fun d(a: Boolean): Boolean { this.d = true; return a }
}
