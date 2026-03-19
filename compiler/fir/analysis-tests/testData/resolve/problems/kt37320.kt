// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-37320
// WITH_STDLIB

// KT-37320: Missing RECURSION_IN_INLINE error when overriding invoke operator

class Shape

sealed class Transformation {
    class NoOpTransformation : Transformation()
    class TransformationComposite(val transformations: List<Transformation>) : Transformation()
}

<!NOTHING_TO_INLINE!>inline<!> operator fun Transformation.invoke(shape: Shape): Shape =
    when (this) {
        is Transformation.NoOpTransformation -> shape
        is Transformation.TransformationComposite -> transformations.fold(shape) { acc, transformation ->
            <!RECURSION_IN_INLINE!>transformation<!>(acc) // Should also produce RECURSION_IN_INLINE
            transformation.<!RECURSION_IN_INLINE!>invoke<!>(acc) // [RECURSION_IN_INLINE] Inline function cannot be recursive
        }
    }

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, inline, isExpression,
lambdaLiteral, nestedClass, operator, primaryConstructor, propertyDeclaration, sealed, smartcast, whenExpression,
whenWithSubject */
