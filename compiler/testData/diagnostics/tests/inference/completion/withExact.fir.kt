// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import kotlin.internal.Exact

class Inv<I>(val arg: I)
class InvExact<E>(val arg: @kotlin.internal.Exact E)

interface Base
class Derived : Base
class Other : Base

fun <K> id(arg: K): K = arg

fun test1(arg: Derived) {
    id<Inv<Base>>(Inv(arg))
    <!INAPPLICABLE_CANDIDATE!>id<!><Inv<Base>>(InvExact(arg))
}

fun <R> Inv<@Exact R>.select(first: R, second: R): R = TODO()

fun test2(derived: Derived, other: Other) {
    Inv(derived).<!INAPPLICABLE_CANDIDATE!>select<!>(derived, other)
}
