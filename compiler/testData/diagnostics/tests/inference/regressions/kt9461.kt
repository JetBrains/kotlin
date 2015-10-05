interface In<in I>

interface B : In<B>

interface C<I, T>

fun <I, T> In<I>.foo(<!UNUSED_PARAMETER!>f<!>: () -> C<I, T>) {}
fun <I, T, Self: In<I>> Self.foo2(<!UNUSED_PARAMETER!>f<!>: () -> C<I, T>) {}

class E : B // e <: In<B> <: In<E>

fun test(c: C<E, Int>, e: E) {
    e.foo<E, Int> { c }
    e.foo { c } // error here: expected C<B, ???> but must be C<??? : B, ???>
    e.foo2 { c }
    e.foo2 ({ c })
}