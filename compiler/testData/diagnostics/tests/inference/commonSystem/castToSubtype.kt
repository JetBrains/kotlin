// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNCHECKED_CAST
// Issue: KT-37914
// DUMP_INFERENCE_LOGS: FIXATION

interface I

fun <R, U : R> castToSubtype(obj: R) = obj as U

fun <T> select(vararg x: T) = x[0]

fun <S1> materialize1(): S1 = null as S1
fun <S2> materialize2(): S2 = null as S2

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

interface Foo<E> : I

class Bar<W>(val x: Foo<W>) : Foo<W>

fun main2() {
    select(
        materialize1<Foo<Any>>(),
        Bar(
            castToSubtype(materialize2<I>()) // NI: "required – Foo<Any>, found – I" afther the commit, OI – OK
        )
    )
}

// Case 2 (using deep supertype)

interface Foo1<Y> : I
interface Foo2<Z> : Foo1<Z>

class Bar1<P>(val x: Foo2<P>) : Foo2<P>

fun main1() {
    select(
        materialize1<Foo2<Any>>(),
        Bar1(
            castToSubtype(materialize2<I>()) // NI: "required – Foo<Any>, found – I" afther the commit, OI – OK
        )
    )
}

/* GENERATED_FIR_TAGS: asExpression, capturedType, classDeclaration, functionDeclaration, integerLiteral,
interfaceDeclaration, nullableType, outProjection, primaryConstructor, propertyDeclaration, typeConstraint,
typeParameter, vararg */
