// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89232
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

sealed interface MySealedInterface {
    object First : MySealedInterface
    object Second : MySealedInterface
}

class WithSealedBound<T : MySealedInterface> {
    fun foo(x: T): Int = when (x) {
        First -> 0
        Second -> 1
        <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> 2
    }

    fun bar(x: T): Boolean = x == First
}

class WithSetBound<T : Set<Int>> {
    fun foo(x: T): Int = when (x) {
        <!CANNOT_INFER_PARAMETER_TYPE!>[]<!> -> 0
        [1] -> 1
        else -> 2
    }

    fun bar(x: T): Boolean = x == [1]
}

class WithIntersectionBound<T> where T : MySealedInterface, T : Comparable<T> {
    fun foo(x: T): Int = when (x) {
        <!UNRESOLVED_REFERENCE!>First<!> -> 0
        else -> 1
    }
}

fun <T : MySealedInterface> generic(x: T): Boolean = x == Second

fun <T : List<Int>> genericList(x: T): Boolean = x == [1]

interface Marker

class WithSetAndMarkerBounds<T> where T : Set<Int>, T : Marker {
    fun foo(x: T): Boolean = x == [1]
}

class WithMarkerAndSealedBounds<T> where T : Marker, T : MySealedInterface {
    fun foo(x: T): Boolean = x == <!UNRESOLVED_REFERENCE!>First<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, integerLiteral, interfaceDeclaration,
nestedClass, objectDeclaration, sealed, smartcast, typeConstraint, typeParameter, whenExpression, whenWithSubject */
