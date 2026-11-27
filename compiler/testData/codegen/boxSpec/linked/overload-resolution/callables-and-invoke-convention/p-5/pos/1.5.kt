// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, callables-and-invoke-convention -> paragraph 5 -> sentence 1
 * PRIMARY LINKS: overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 2
 * overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 7
 * NUMBER: 5
 * DESCRIPTION: function-like prio is higher than property-like callables
 */

//function-like (I prio)
var fooFun: Boolean = false
fun foo() { fooFun = true }

//property-like (II prio)
var isMarker = false
val foo: Marker = object : Marker {}
interface Marker {
    operator fun invoke() { isMarker = true }
}

fun box(): String {
    foo()
    if (!isMarker && fooFun)
        return "OK"
    return "NOK"
}