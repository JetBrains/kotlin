// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed interface Either<out E, out A> {
    data class  Left<out E>(val error: E): Either<E, Nothing>
    data class Right<out A>(val value: A): Either<Nothing, A>
}

fun foo(s: Either<String, Int>) {
    when {
        s is Left -> s.error.length
        s is Right -> s.value * 1
    }

    if (s !is Left) {
        s.hashCode()
    }

    if (1 == "".hashCode()) {
        s as Left
        s.error.length
    }

    (s as? Right)?.value?.div(1)
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, data, equalityExpression, functionDeclaration, ifExpression,
integerLiteral, interfaceDeclaration, isExpression, multiplicativeExpression, nestedClass, nullableType, out,
primaryConstructor, propertyDeclaration, safeCall, sealed, smartcast, stringLiteral, typeParameter, whenExpression */
