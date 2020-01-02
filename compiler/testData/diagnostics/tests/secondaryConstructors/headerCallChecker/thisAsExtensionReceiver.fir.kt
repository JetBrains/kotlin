// !DIAGNOSTICS: -UNUSED_PARAMETER

fun A.foobar() = 1
val A.prop: Int get() = 2

class A {
    constructor(x: Int)
    constructor() : this(
            foobar() +
            this.foobar() +
            prop +
            this.prop +
            this@A.prop
    )
}
