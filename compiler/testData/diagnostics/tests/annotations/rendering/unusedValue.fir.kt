// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE +UNUSED_VALUE

@Target(AnnotationTarget.PROPERTY,  AnnotationTarget.FUNCTION, AnnotationTarget.TYPE,  AnnotationTarget.LOCAL_VARIABLE)
annotation class A

@A
fun test() {
    @A
    var b: @A Int = 0
    b = 15
}

/* GENERATED_FIR_TAGS: annotationDeclaration, assignment, functionDeclaration, integerLiteral, localProperty,
propertyDeclaration */
