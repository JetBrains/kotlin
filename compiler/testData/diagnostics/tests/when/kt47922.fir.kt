// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-47922

package whencase.castissue

sealed class SealedBase {
    object Complete : SealedBase()
}

abstract class NonSealedBase {
    object Complete : NonSealedBase()
}

sealed class ToState

val sealedTest: SealedBase.() -> ToState? = {
    <!RETURN_TYPE_MISMATCH!><!NO_ELSE_IN_WHEN!>when<!>(this) {}<!>
}

val nonSealedTest: NonSealedBase.() -> ToState? = {
    <!RETURN_TYPE_MISMATCH!>when(this) {}<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionalType, lambdaLiteral, nestedClass, nullableType, objectDeclaration,
propertyDeclaration, sealed, typeWithExtension, whenExpression, whenWithSubject */
