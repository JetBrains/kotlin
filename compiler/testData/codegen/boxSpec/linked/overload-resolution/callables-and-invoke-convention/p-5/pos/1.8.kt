// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, callables-and-invoke-convention -> paragraph 5 -> sentence 1
 * PRIMARY LINKS: overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 2
 * overload-resolution, callables-and-invoke-convention -> paragraph 2 -> sentence 8
 * NUMBER: 8
 * DESCRIPTION: function-like prio is higher than property-like callables
 */

class MyClass {

    //function-like (I prio)
    var fooFun: Boolean = false
    fun foo() { fooFun = true }

    //property-like (II prio)
    companion object foo {
        var fooCompanionObj = false
        operator fun invoke() {
            fooCompanionObj = true
        }
    }

    fun check() : String {
        foo()
        if (fooFun && !fooCompanionObj)
            return "OK"
        return "NOK"
    }
}

fun box() : String{
    return MyClass().check()
}