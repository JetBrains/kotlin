// "Specify supertype" "true"
package a.b.c

interface Z {
    fun foo() {}
}

open class X {
    open fun foo() {}
}

class Test : (@Suppress("foo") a.b.c.X)(), Z {
    override fun foo() {
        <caret>super.foo()
    }
}