// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed interface Either<out E, out A> {
    data class  Left<out E>(val error: E): Either<E, Nothing>
    data class Right<out A>(val value: A): Either<Nothing, A>
}

fun <E, A> Either<E, A>.getOrElse(default: A) = <!WHEN_ON_SEALED_GEEN_ELSE!>when (this) {
    is Left -> default
    is Right -> value
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, data, funWithExtensionReceiver, functionDeclaration, interfaceDeclaration,
isExpression, nestedClass, nullableType, out, primaryConstructor, propertyDeclaration, sealed, smartcast, typeParameter,
whenExpression, whenWithSubject */
