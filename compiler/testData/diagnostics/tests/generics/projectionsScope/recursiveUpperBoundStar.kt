// !DIAGNOSTICS: -UNUSED_PARAMETER
// See KT-7296
interface A<T>
interface B<T> : A<A<T>>

fun foo(x : B<*>) {
    bar(x) // this should not be valid
}

fun bar(x : A<A<*>>) { }
