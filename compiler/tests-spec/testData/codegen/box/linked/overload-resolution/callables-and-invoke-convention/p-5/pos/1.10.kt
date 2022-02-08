// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, callables-and-invoke-convention -> paragraph 5 -> sentence 1
 * PRIMARY LINKS: overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 4
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
open class A()
class boo() : A() {}

fun box(): String {
    val x: Any = boo()
    if (x !is A && test.lib.MyClass.fooCompanionObj)
        return "OK"
    return "NOK"
}