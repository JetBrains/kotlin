// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-253
 * PLACE: overload-resolution, callables-and-invoke-convention -> paragraph 6 -> sentence 1
 * RELEVANT PLACES: overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 4
 * overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 6
 * NUMBER: 3
 * DESCRIPTION: function-like prio is higher than property-like callables
 */


class foo(val a: Int)

var fooLambda: Boolean = false

val foo: (Int) -> Unit = { a: Int -> fooLambda = true }


fun box(): String {

    val x = foo(1)

    if (x is foo && !fooLambda)
        return "OK"

    return "NOK"
}