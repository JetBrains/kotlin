// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, callables-and-invoke-convention -> paragraph 5 -> sentence 1
 * PRIMARY LINKS: overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 2
 * overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 6
 * NUMBER: 1
 * DESCRIPTION: function-like prio is higher than property-like callables
 */

// FILE: KotlinLib.kt
package test.lib

var booInt: Boolean = false
var booLambda: Boolean = false

fun boo(a: Int) { booInt = true }
val boo: (Int) -> Unit = { a: Int -> booLambda = true}

// FILE: KotlinClass.kt
package overloadResolution

var fooInt: Boolean = false
var fooLambda: Boolean = false

fun foo(a: Int) { fooInt = true }
val foo: (Int) -> Unit = { a: Int -> fooLambda = true}


fun box(): String {

    overloadResolution.foo(1)

    if (fooInt && ! fooLambda)
        return "OK"

    return "NOK"
}