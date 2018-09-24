// !WITH_NEW_INFERENCE
interface A<T>
interface B<E> {
    fun foo(x: A<in E>)
}

fun foo(x: B<in CharSequence>, y: A<CharSequence>) {
    x.foo(<!NI;TYPE_MISMATCH, OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>y<!>)
}
