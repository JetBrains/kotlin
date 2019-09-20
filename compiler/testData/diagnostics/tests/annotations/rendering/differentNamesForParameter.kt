// !RENDER_DIAGNOSTICS_MESSAGES

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

<!DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES("public abstract fun foo(a: Int): Unit defined in A, public abstract fun foo(b: Int): Unit defined in B", "1")!>interface C<!> : A, B