// FIR_IDENTICAL
// !DIAGNOSTICS: -UPPER_BOUND_VIOLATED

interface A<T>
interface B<T> : A<A<*>>

interface N0<in T>
interface C0<<!EXPANSIVE_INHERITANCE!>X<!>> : N0<N0<C0<C0<X>>>>

interface N1<in T>
interface C1<<!EXPANSIVE_INHERITANCE!>X<!>> : N1<N1<C1<C1<X?>>>>
interface C2<T> : C1<C1<T>>

interface C<<!EXPANSIVE_INHERITANCE!>X<!>> : D<P<X, X>>
interface P<Y1, <!EXPANSIVE_INHERITANCE!>Y2<!>> : Q<C<Y1>, C<D<Y2>>>
interface Q<Z1, Z2>
interface D<W>

interface E0<T>
interface E1<T : E2>
interface E2 : E0<E1<out E2>>

interface F0<T>
interface F1<T : F2<*>, U : F2<*>>
interface F2<T> : F0<F1<out F2<*>, T>>

interface G0<T>
interface G1<T : U, U : G2<*>>
interface G2<T> : G0<G1<out G2<*>, T>>

abstract class H0<X> {
    inner abstract class H1<Y> : H0<H1<Y>>() // Outer(X) should not be reported as non-expansive
}
