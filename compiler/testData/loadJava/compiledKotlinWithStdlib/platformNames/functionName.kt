// PLATFORM_DEPENDANT_METADATA
package test

annotation class A(val s: String)

@A("1")
@JvmName("bar")
fun foo() = "foo"

@field:A("2")
var v: Int = 1
    @A("3")
    @JvmName("vget")
    get
    @A("4")
    @JvmName("vset")
    set
