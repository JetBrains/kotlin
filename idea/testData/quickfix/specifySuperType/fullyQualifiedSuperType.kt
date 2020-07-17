// "Specify supertype" "true"
package a.b.c

interface X

open class Y {
    open fun foo() {}
}

interface Z {
    fun foo() {}
}

class Test : X, a.b.c.Y(), Z {
    override fun foo() {
        <caret>super.foo()
    }
}