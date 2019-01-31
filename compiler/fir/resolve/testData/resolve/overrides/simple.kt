

open class A {
    open fun foo(): A = this
    open fun bar(): A = this
}

class B : A() {
    override fun foo(): B = this
    fun bar(): B = this // Ambiguity, no override here

    fun test() {
        foo()
        bar()
    }
}

