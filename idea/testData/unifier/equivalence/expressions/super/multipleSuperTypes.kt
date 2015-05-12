interface T {
    fun foo() {

    }
}

open class Z {
    open fun foo() {

    }
}

class A: Z(), T {
    override fun foo() {
        <selection>super<T>.foo()</selection>
        super<Z>.foo()
    }
}