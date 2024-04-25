// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// NI_EXPECTED_FILE

interface In<in E>
open class A : In<A>
open class B : In<B>

fun <T> select(x: T, y: T) = x

fun foo2() = select(A(), B()) // Type "In<A & B>" is prohibited in return position



open class C : In<C>
open class D : In<D>
open class E : In<E>
open class F : In<F>
open class G : In<G>
open class H : In<H>

fun <S> select8(a: S, b: S, c: S, d: S, e: S, f: S, g: S, h: S) = a

fun foo8() = select8(A(), B(), C(), D(), E(), F(), G(), H())
