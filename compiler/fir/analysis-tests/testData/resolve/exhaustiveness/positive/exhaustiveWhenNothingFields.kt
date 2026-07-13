// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-77979
// WITH_STDLIB

sealed interface Adt<A, B>
data class A<A, B>(val x: B) : Adt<A, B>
sealed interface BAdt<B, A> : Adt<A, B>
data class B<A, B>(val x: B) : BAdt<A, B>
data class C<A, B>(val x: A) : BAdt<A, B>

fun example(a: Adt<Nothing, String>) {
    when (a) {
        is A -> {}
        is C -> {}
        // no need in case B
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, interfaceDeclaration, isExpression, nullableType,
primaryConstructor, propertyDeclaration, sealed, smartcast, typeParameter, whenExpression, whenWithSubject */
