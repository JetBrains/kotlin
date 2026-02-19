// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

open class ListA<A> {
    companion object {
        operator fun <A> of(vararg values: A): ListA<A> = ListA()
    }
}

class ListB<B> : ListA<B>() {
    companion object {
        operator fun <B> of(vararg values: B): ListB<B> = ListB()
    }
}

fun <X> select(vararg x: X): X = x[0]
fun <Y: ListA<*>> selectWithStarDUB(vararg y: Y): Y = y[0]
fun <Z: ListA<Int>> selectWithDUB(vararg z: Z): Z = z[0]

fun testSelect() {
    <!CANNOT_INFER_PARAMETER_TYPE!>select<!>(ListA.<!CANNOT_INFER_PARAMETER_TYPE!>of<!>(), <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    select(ListA.of<Int>(), [])
    select(ListA.of(), [42])
    select(ListA.of<Int>(), [42])
    val p1: ListA<*> = select(ListB.<!CANNOT_INFER_PARAMETER_TYPE!>of<!>(), <!AMBIGUOUS_COLLECTION_LITERAL!>[]<!>)
    val p2: ListA<Int> = select(ListB.of(), <!AMBIGUOUS_COLLECTION_LITERAL!>[]<!>)
    val p3: ListA<*> = select(ListB.<!CANNOT_INFER_PARAMETER_TYPE!>of<!>(), <!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!>)
    val p4: ListA<*> = select(ListB.of<Int>(), <!AMBIGUOUS_COLLECTION_LITERAL!>[]<!>)
    val p5: ListA<Int> = select(ListB.of<Int>(), <!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!>)
    val p6: ListA<*> = select(ListB.of<Int>(), <!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!>)
    val p7: ListA<Int> = select(ListB.of(), <!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!>)
    val p8: ListA<Int> = select(ListB.of<Int>(), <!AMBIGUOUS_COLLECTION_LITERAL!>[]<!>)
}

fun testSelectWithUpperBounds() {
    selectWithStarDUB(ListB.of<Int>(), <!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!>)
    selectWithDUB(ListB.of<Int>(), <!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!>)
}

fun testWhen() {
    if (true) <!CANNOT_INFER_PARAMETER_TYPE!>[]<!> else ListA.<!CANNOT_INFER_PARAMETER_TYPE!>of<!>()
    if (true) [] else ListA.of<Int>()
    if (true) [42] else ListA.of()
    if (true) [42] else ListA.of<Int>()

    val p1: ListA<*> = if (true) <!AMBIGUOUS_COLLECTION_LITERAL!>[]<!> else ListB.<!CANNOT_INFER_PARAMETER_TYPE!>of<!>()
    val p2: ListA<Int> = if (true) <!AMBIGUOUS_COLLECTION_LITERAL!>[]<!> else ListB.of()
    val p3: ListA<*> = if (true) <!AMBIGUOUS_COLLECTION_LITERAL!>[]<!> else ListB.of(42)
    val p4: ListA<Int> = if (true) <!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!> else ListB.of()
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, collectionLiteral, companionObject, functionDeclaration,
ifExpression, integerLiteral, localProperty, nullableType, objectDeclaration, operator, outProjection,
propertyDeclaration, starProjection, typeConstraint, typeParameter, vararg */
