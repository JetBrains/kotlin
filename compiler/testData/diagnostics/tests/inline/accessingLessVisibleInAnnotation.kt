// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
internal class IntrinsicType {
    companion object {
        const val PLUS = "PLUS"
    }
}

annotation class Ann(val value: String)

@Ann(IntrinsicType.PLUS)
<!NOTHING_TO_INLINE!>inline<!> fun foo() {}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, companionObject, const, functionDeclaration, inline,
objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
