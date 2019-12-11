// !DIAGNOSTICS: -UNUSED_PARAMETER

fun A.foobar() = 3

class A {
    fun foo() = 1
    constructor( x: Any = object {
        fun bar() = foo() + this@A.foo() +
                    foobar()
    })
}
