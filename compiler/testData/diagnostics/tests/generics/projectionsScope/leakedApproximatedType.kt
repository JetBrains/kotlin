// !DIAGNOSTICS: -UNUSED_PARAMETER

class Out<out T>
class In<in T> {
    fun invoke1(x: T) {}
    fun invoke2(x: Out<T>) {}
}

interface A<E> {
    fun foo(): In<E>
}

fun test(a: A<out CharSequence>, y: Out<CharSequence>) {
    val i = a.foo()
    // TODO: These diagnostic are wrong, type of 'i' --- 'In<Nothing>' is not projected itself,
    // but it's approximation result caused by 'a' projection
    i.invoke1(<!TYPE_MISMATCH!>""<!>)
    i.invoke2(<!TYPE_MISMATCH!>y<!>)
}
