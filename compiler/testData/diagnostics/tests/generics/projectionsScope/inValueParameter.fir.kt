// !WITH_NEW_INFERENCE
interface A<T>
interface B<E> {
    fun foo(x: A<in E>)
}

fun foo(x: B<in CharSequence>, y: A<CharSequence>) {
    x.foo(<!ARGUMENT_TYPE_MISMATCH!>y<!>)
}
