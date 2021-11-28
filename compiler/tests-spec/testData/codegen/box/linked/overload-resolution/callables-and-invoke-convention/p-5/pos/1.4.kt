// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, callables-and-invoke-convention -> paragraph 5 -> sentence 1
 * PRIMARY LINKS: overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 2
 * overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 10
 * NUMBER: 4
 * DESCRIPTION: function-like prio is higher than property-like callables
 */
 
// FILE: KotlinLib.kt
package test.lib

var isFooCalled: Boolean = false
val foo: (Int) -> Unit = { a: Int -> isFooCalled = true}

// FILE: KotlinClass.kt
package overloadResolution
import test.lib.foo as boo

var isBooCalled: Boolean = false
fun boo(a: Int) { isBooCalled = true }

fun box(): String {

    boo(1)

    if (test.lib.isFooCalled && !isBooCalled)
        return "OK"

    return "NOK"
}