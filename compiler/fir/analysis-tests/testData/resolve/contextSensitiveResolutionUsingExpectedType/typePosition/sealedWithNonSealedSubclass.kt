// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed interface MySealed {
    class Left(val x: String): MySealed
    class Right(val y: String): MySealed
    class NonSealedSubclass(val z: String)
}

fun MySealed.getOrElse() = <!WHEN_ON_SEALED!>when (this) {
    is Left -> x
    is Right -> y
    is <!UNRESOLVED_REFERENCE!>NonSealedSubclass<!> -> <!UNRESOLVED_REFERENCE!>z<!>
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, interfaceDeclaration,
isExpression, nestedClass, primaryConstructor, propertyDeclaration, sealed, smartcast, whenExpression, whenWithSubject */
