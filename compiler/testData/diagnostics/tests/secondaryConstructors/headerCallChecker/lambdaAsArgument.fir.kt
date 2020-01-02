// !DIAGNOSTICS: -UNUSED_PARAMETER

fun A.foobar() = 3

class A {
    fun foo() = 1
    constructor(x: () -> Int)
    constructor() : this(
            {
                foo() +
                this.foo() +
                this@A.foo() +
                foobar()
            })
}
