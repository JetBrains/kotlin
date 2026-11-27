// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, callables-and-invoke-convention -> paragraph 5 -> sentence 1
 * PRIMARY LINKS: overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 6
 * overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 3
 * NUMBER: 2
 * DESCRIPTION: function-like prio is higher than property-like callables
 */
 
// FILE: KotlinLib.kt
package test.lib

var isBooCalled: Boolean = false

fun boo(a: Int) { isBooCalled = true }


// FILE: KotlinClass.kt
package overloadResolution
import test.lib.boo as foo

var isFooCalled: Boolean = false

val foo: (Int) -> Unit = { a: Int -> isFooCalled = true}


fun box(): String {

    foo(1)

    if (!isFooCalled && test.lib.isBooCalled)
        return "OK"

    return "NOK"
}