// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS,  AnnotationTarget.PROPERTY,  AnnotationTarget.VALUE_PARAMETER)
annotation class An

@An
data class A(@An val x: @An Int) {
    <!CONFLICTING_OVERLOADS("public final fun copy(x: Int): Int defined in A")!>@An
    fun copy(@An x: @An Int)<!> = x
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, data, functionDeclaration, primaryConstructor,
propertyDeclaration */
