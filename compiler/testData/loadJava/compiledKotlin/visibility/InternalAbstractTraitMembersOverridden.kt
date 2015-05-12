//ALLOW_AST_ACCESS
package test

interface A {
    internal fun f() : Int
    internal val v : Int
    public var p : Int
}

class B : A {
    override fun f(): Int = throw UnsupportedOperationException()
    public override var p: Int = 0
    override val v: Int = 0
}
