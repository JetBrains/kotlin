// FIR_IDENTICAL
// ISSUE: KT-64841
abstract class A<X, Y : A<X, Y>>

abstract class B<X, T, Y : B<X, T, Y>>(delegate: A<X, *>) : A<X, Y>() {
    inner class C<R> : B<X, R, C<R>>(this)
}
