// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

interface MyListA<A> {
    companion object {
        operator fun <A> of(vararg t: A): MyListA<A> = object : MyListA<A> { }
    }
}

interface MyListB<B> {
    companion object {
        operator fun <B> of(vararg t: B): MyListB<B> = object : MyListB<B> { }
    }
}

interface MyListC<C>: MyListA<C>, MyListB<C> {
    companion object {
        operator fun <C> of(vararg t: C): MyListC<C> = object : MyListC<C> { }
    }
}

fun <T: MyListA<*>> idA(t: T): T = t
fun <U: MyListC<*>> idC(u: U): U = u

fun testIdA() {
    // no non-declared upper bounds
    val x = idA([42])

    // non-declared upper bound is `MyListB<*>`, `MyListA<*> & MyListB<*>` is used for lookup
    // ==> fallback
    val y: MyListB<*> = idA(<!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!>)

    // MyListA.of
    val z: MyListA<*> = idA([42])

    // non-declared is `MyListC<*>`, which is subclass of `MyListA`, so we can use `MyListC.of`
    val t: MyListC<*> = idA([42])
}

fun testIdC() {
    val x = idC([42])

    // non-declared is `MyListA<*>`, but we must use declared to resolve to `MyListC.of`
    val y: MyListA<*> = idC([42])
}

/* ========= */

interface MyListOutA<out A> {
    companion object {
        operator fun <A> of(vararg a: A): MyListOutA<A> = object : MyListOutA<A> { }
    }
}

interface MyListOutB<out B> : MyListOutA<B> {
    companion object {
        operator fun <B> of(vararg b: B): MyListOutB<B> = object : MyListOutB<B> { }
    }
}

interface X
fun xx(): X = object : X {}
interface Y
fun yy(): Y = object : Y {}
interface Z : X, Y
fun zz(): Z = object : Z {}

fun <T: MyListOutA<X>> idOutA(t: T): T = t
fun <U: MyListOutB<X>> idOutB(u: U): U = u

fun testIdOutA() {
    idOutA([])
    <!CANNOT_INFER_PARAMETER_TYPE!>idOutA<!>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)
    idOutA([xx()])
    idOutA([zz()])

    val p0: MyListOutA<Z> = idOutA([])
    val p1: MyListOutA<Y> = idOutA([])
    val p2: MyListOutA<Y> = idOutA([zz()])
    val p3: MyListOutA<Y> = idOutA(<!ARGUMENT_TYPE_MISMATCH!>[yy()]<!>)

    val p4: MyListOutB<X> = idOutA([])
    val p5: MyListOutB<Z> = idOutA([])
    val p6: MyListOutB<Y> = idOutA([])
}

fun testIdOutB() {
    val p1: MyListOutA<X> = idOutB([])
    val p2: MyListOutA<Y> = idOutB([])
    val p3: MyListOutA<Z> = idOutB([])
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, collectionLiteral, companionObject, functionDeclaration,
integerLiteral, interfaceDeclaration, intersectionType, localProperty, nullableType, objectDeclaration, operator, out,
propertyDeclaration, starProjection, typeConstraint, typeParameter, vararg */
