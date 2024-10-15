// RUN_PIPELINE_TILL: SOURCE
interface A<T>
interface B<E> {
    fun foo(x: A<in E>)
}

fun foo(x: B<in CharSequence>, y: A<CharSequence>) {
    x.foo(<!TYPE_MISMATCH!>y<!>)
}
