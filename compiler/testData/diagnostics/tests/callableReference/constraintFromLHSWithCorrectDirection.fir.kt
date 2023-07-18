// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty1

interface A
inline val <T : A> T.bla get() = 1

class B<T>
fun <K, V> B<K>.foo(p: KProperty1<in K, V>): B<V> = TODO()

fun <K, V> B<out K>.bar(p: KProperty1<out K, V>): B<V> = TODO()

fun <K, V> B<K>.baz(p: KProperty1<out K, V>): B<V> = TODO()

fun <K, V> B<K>.star(p: KProperty1<*, V>): B<V> = TODO()


fun <R : A> B<R>.test(){
    foo(A::bla)
    bar(A::bla)
    baz(A::<!UNRESOLVED_REFERENCE!>bla<!>)
    star(A::bla)
}
