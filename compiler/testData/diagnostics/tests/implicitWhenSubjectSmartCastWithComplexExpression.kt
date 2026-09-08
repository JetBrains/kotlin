// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-87788

sealed class KtSourceElementKind {
    abstract val shouldSkipErrorTypeReporting: Boolean
}

sealed class KtSourceElement {
    abstract val kind: KtSourceElementKind
}

data object KtRealSourceElementKind : KtSourceElementKind() {
    override val shouldSkipErrorTypeReporting: Boolean
        get() = false
}

class SourceCodeAnalysisException(val source: KtSourceElement, override val cause: Throwable) : Exception() {
    override val message get() = ""
}

fun Throwable.wrapIntoSourceCodeAnalysisExceptionIfNeeded(element: KtSourceElement?): Throwable =
	// The safe call produces `(<element?.kind> != Null) implies (<element>: Any)`
    when (element?.kind) {
        // The branch body gives `<subject> != Null`
        is KtRealSourceElementKind -> SourceCodeAnalysisException(element, this)
        else -> this
    }

/* GENERATED_FIR_TAGS: classDeclaration, data, funWithExtensionReceiver, functionDeclaration, getter, isExpression,
nullableType, objectDeclaration, override, primaryConstructor, propertyDeclaration, sealed, stringLiteral,
thisExpression, whenExpression, whenWithSubject */
