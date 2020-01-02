// !DIAGNOSTICS: -UNUSED_PARAMETER

fun A.foobar() = 3

class A {
    fun foo() = 1
    constructor(x: Any?)
    constructor() : this(object {
        fun bar() = foo() + this@A.foo() +
                    foobar() + super@A.hashCode()
    })
}
