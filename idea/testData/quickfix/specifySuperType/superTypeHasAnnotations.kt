// "Specify supertype" "true"
interface Z {
    fun foo() {}
}

open class X {
    open fun foo() {}
}

class Test : (@Suppress("foo") X)(), Z {
    override fun foo() {
        <caret>super.foo()
    }
}