// FIR_IDENTICAL
// ISSUE: KT-64222

interface A {
    fun bar(): B<Int>
}

interface B<T>
fun <E> foo(block: B<E>.() -> Unit): B<E> = TODO()

class C : A {
    private var value: B<Int>? = null

    override fun bar() = foo {
        value = this
    }
}
