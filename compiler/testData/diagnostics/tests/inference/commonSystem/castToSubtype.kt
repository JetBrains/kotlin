// FIR_IDENTICAL
// DIAGNOSTICS: -UNCHECKED_CAST
// Issue: KT-37914

interface I

fun <R, U : R> castToSubtype(obj: R) = obj as U

fun <T> select(vararg x: T) = x[0]

fun <S> materialize(): S = null as S

// Case 1 (using intermediate supertype)

/*
 Constraint system:
     R:
        TypeVariable(U) <: TypeVariable(R)
        I <: TypeVariable(R)
     U:
        I >: TypeVariable(U)
        Foo<TypeVariable(P)> >: TypeVariable(U)
     T:
        Foo<Any> <: TypeVariable(T)
        Bar<TypeVariable(P)> <: TypeVariable(T)

 Fixation order before the fix: R, U, T, P
 Fixation order after the fix: R, T, P, U

 `U` has begun to have lower priority so it can be fixed to more specific type (to `Foo<Any>` instead of `I`).
 */

interface Foo<T> : I

class Bar<T>(val x: Foo<T>) : Foo<T>

fun main2() {
    select(
        materialize<Foo<Any>>(),
        Bar(
            castToSubtype(materialize<I>()) // NI: "required – Foo<Any>, found – I" afther the commit, OI – OK
        )
    )
}

// Case 2 (using deep supertype)

interface Foo1<Y> : I
interface Foo2<Y> : Foo1<Y>

class Bar1<P>(val x: Foo2<P>) : Foo2<P>

fun main1() {
    select(
        materialize<Foo2<Any>>(),
        Bar1(
            castToSubtype(materialize<I>()) // NI: "required – Foo<Any>, found – I" afther the commit, OI – OK
        )
    )
}