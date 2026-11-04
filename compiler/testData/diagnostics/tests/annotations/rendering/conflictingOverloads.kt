// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS,  AnnotationTarget.PROPERTY,  AnnotationTarget.VALUE_PARAMETER)
annotation class An

@An
data class <!CONFLICTING_OVERLOADS("In package 'root package' of module '<main>': --> conflictingOverloads.kt  |7 | data class A : Any {  | ...9 |     fun copy(x: Int): Int  | ...  | }  |")!>A(@An val x: @An Int)<!> {
    @An
    <!CONFLICTING_OVERLOADS("In package 'root package' of module '<main>': --> conflictingOverloads.kt  |7 | data class A : Any {8 |     fun copy(x: Int = ...): A  | ...  | }  |")!>fun copy(@An x: @An Int)<!> = x
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, data, functionDeclaration, primaryConstructor,
propertyDeclaration */
