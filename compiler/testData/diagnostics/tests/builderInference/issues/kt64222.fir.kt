// ISSUE: KT-64222

interface A {
    fun bar(): B<Int>
}

interface B<T>
fun <E> foo(block: B<E>.() -> Unit): B<E> = TODO()

class C : A {
    private var value: B<Int>? = null

    override fun <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>bar<!>() = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!> {
        value = this
    }
}
