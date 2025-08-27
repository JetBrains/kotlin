// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed interface MySealed {
    class Left(val x: String): MySealed
    class Right(val y: String): MySealed
}

interface Left {
    val z: String
}

fun MySealed.getOrElse() = when (this) {
    is Left -> z
    is Right -> y
    else -> ""
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, interfaceDeclaration,
intersectionType, isExpression, nestedClass, primaryConstructor, propertyDeclaration, sealed, smartcast, stringLiteral,
whenExpression, whenWithSubject */
