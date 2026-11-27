// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, callables-and-invoke-convention -> paragraph 5 -> sentence 1
 * PRIMARY LINKS: overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 2
 * overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 8
 * overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 10
 * NUMBER: 9
 * DESCRIPTION: function-like prio is higher than property-like callables
 */

// FILE: KotlinLib.kt
package test.lib
class MyClass {
    //property-like (II prio)
    companion object foo {
        var fooCompanionObj = false
        operator fun invoke() {
            fooCompanionObj = true
        }
    }
}

// FILE: KotlinClass.kt
package overloadResolution
import test.lib.MyClass.foo as boo

//function-like (I prio)
var booFun: Boolean = false
fun boo() { booFun = true }

fun box(): String {
    boo()
    if (!booFun && test.lib.MyClass.fooCompanionObj)
        return "OK"
    return "NOK"
}