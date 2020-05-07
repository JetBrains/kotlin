// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    fun foo() = 1
    constructor(x: Int)
    constructor(x: Int, y: Int, z: Int = x + foo() + this.foo()) :
        this(x + foo() + this.foo())
}
