// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89232
// FIR_DUMP
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

sealed interface MySealedInterface {
    object First : MySealedInterface
    object Second : MySealedInterface
}

interface Marker

class Sub : MySealedInterface, Marker

fun testSmartCast(x: Any, l: Any) {
    if (x is MySealedInterface && x is Marker) {
        x == <!UNRESOLVED_REFERENCE!>First<!>
        when (x) {
            <!UNRESOLVED_REFERENCE!>First<!> -> 0
            else -> 1
        }
    }

    if (x is Sub && <!USELESS_IS_CHECK!>x is Marker<!>) {
        x == <!UNRESOLVED_REFERENCE!>First<!>
    }

    if (l is <!CANNOT_CHECK_FOR_ERASED!>Collection<Int><!> && l is List<Int>) {
        l == [1]
        when (l) {
            <!CANNOT_INFER_PARAMETER_TYPE!>[]<!> -> 0
            else -> 1
        }
    }
}

fun <T> takeIntersection(x: T) where T : MySealedInterface, T : Marker {}

fun testGeneric(sub: Sub) {
    <!CANNOT_INFER_PARAMETER_TYPE!>takeIntersection<!>(<!ARGUMENT_TYPE_MISMATCH!>First<!>)
    takeIntersection(sub)
}

open class Box {
    companion object {
        operator fun of(vararg elements: Int): Box = TODO()
    }
}

fun <T> select(x: T, y: T): T = x

fun testIntersectionAsBound(x: Any) {
    if (x is Box && x is Marker) {
        x == [1]
        select(x, [1])
    }
    if (x is MySealedInterface && x is Marker) {
        select(x, <!UNRESOLVED_REFERENCE!>First<!>)
    }
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, companionObject, equalityExpression, functionDeclaration,
ifExpression, integerLiteral, interfaceDeclaration, intersectionType, isExpression, nestedClass, nullableType,
objectDeclaration, operator, sealed, smartcast, typeConstraint, typeParameter, vararg, whenExpression, whenWithSubject */
