// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS,  AnnotationTarget.PROPERTY,  AnnotationTarget.VALUE_PARAMETER)
annotation class An

@An
data class <!CONFLICTING_OVERLOADS("fun copy(x: Int): Int")!>A(<!ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD("property")!>@An<!> val x: @An Int)<!> {
    @An
    <!CONFLICTING_OVERLOADS("fun copy(x: Int = ...): A")!>fun copy(@An x: @An Int)<!> = x
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, data, functionDeclaration, primaryConstructor,
propertyDeclaration */
