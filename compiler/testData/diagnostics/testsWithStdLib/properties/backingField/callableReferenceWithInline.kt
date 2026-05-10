// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE

class InlineWithCallableReference {
    val a: Any field: String = ""

    internal inline fun foo(): String = <!RETURN_TYPE_MISMATCH!>::a.get()<!>
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, explicitBackingField, functionDeclaration, inline,
propertyDeclaration, stringLiteral */
