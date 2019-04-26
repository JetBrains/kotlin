class C
class D

open class A {
    open fun foo(): A = this
    open fun bar(): A = this
    open fun buz(p: A): A = this
}

class B : A() {
    override fun foo(): B = this
    fun bar(): B = this // Ambiguity, no override here (really it's just "missing override" and no ambiguity)
    override fun buz(p: C): B = this //No override as C <!:> A

    fun test() {
        foo()
        bar()
        buz(D())
    }
}

