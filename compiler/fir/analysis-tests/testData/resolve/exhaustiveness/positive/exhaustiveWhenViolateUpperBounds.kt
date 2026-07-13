// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-77979
// WITH_STDLIB

sealed interface Adt<out A>
data class A<A: Number>(val x: A) : Adt<A>
data class B(val x: Unit) : Adt<Nothing>

fun example(a: Adt<String>) {
    when (a) {
        is B -> {}
        // no need in case A
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, interfaceDeclaration, isExpression, nullableType,
out, primaryConstructor, propertyDeclaration, sealed, typeConstraint, typeParameter, whenExpression, whenWithSubject */
