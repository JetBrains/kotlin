// !WITH_NEW_INFERENCE
interface A<T>
interface B<E> {
    fun foo(x: A<in E>)
}

fun foo(x: B<in CharSequence>, y: A<CharSequence>) {
    x.<!INAPPLICABLE_CANDIDATE!>foo<!>(y)
}
