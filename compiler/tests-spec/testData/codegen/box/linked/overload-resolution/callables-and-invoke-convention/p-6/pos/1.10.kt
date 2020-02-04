// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-253
 * PLACE: overload-resolution, callables-and-invoke-convention -> paragraph 6 -> sentence 1
 * RELEVANT PLACES: overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 4
 * overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 9
 * NUMBER: 10
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
class boo(){}

fun box(): String {
   val x =  boo()
    if (x is boo && !test.lib.MyClass.fooCompanionObj)
        return "OK"
    return "NOK"
}