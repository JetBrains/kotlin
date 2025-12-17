// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed interface MySealed {
    class Left(val x: String): MySealed
    class Right(val y: String): MySealed
}

fun MySealed.getOrElse() = <!WHEN_ON_SEALED_GEEN_ELSE!>when (this) {
    is Left -> x
    is Right -> y
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, interfaceDeclaration,
isExpression, nestedClass, primaryConstructor, propertyDeclaration, sealed, smartcast, whenExpression, whenWithSubject */
