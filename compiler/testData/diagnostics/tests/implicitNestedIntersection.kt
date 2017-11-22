// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface In<in E>
open class A : In<A>
open class B : In<B>

fun <T> select(x: T, y: T) = x

fun <!OI;IMPLICIT_INTERSECTION_TYPE!>foo2<!>() = select(A(), B()) // Type is In<A & B> is prohibited in return position
