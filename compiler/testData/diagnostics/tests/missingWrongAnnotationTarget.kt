// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// ISSUE: KT-53565

@Target(AnnotationTarget.CLASS)
annotation class InvalidAnn

fun wrongAnnotationTargetInWhenSubject() {
    // Error expected on annotation
    when(<!WRONG_ANNOTATION_TARGET!>@InvalidAnn<!> val x = 0) {
        0 -> {}
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, equalityExpression, functionDeclaration, integerLiteral, localProperty,
propertyDeclaration, whenExpression, whenWithSubject */
