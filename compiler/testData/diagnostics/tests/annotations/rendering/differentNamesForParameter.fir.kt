// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTICS_MESSAGES

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS,  AnnotationTarget.PROPERTY,  AnnotationTarget.VALUE_PARAMETER)
annotation class An

@An
interface A {
    @An
    fun foo(@An a : @An Int)
}

@An
interface B {
    @An
    fun foo(@An b : @An Int)
}

<!DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES("a; b; 0; 'fun foo(a: @An() Int): Unit' defined in '/A', 'fun foo(b: @An() Int): Unit' defined in '/B'")!>interface C<!> : A, B
