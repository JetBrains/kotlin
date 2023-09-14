// !RENDER_DIAGNOSTICS_MESSAGES
// !DIAGNOSTICS: -ABSTRACT_MEMBER_NOT_IMPLEMENTED

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS,  AnnotationTarget.VALUE_PARAMETER,  AnnotationTarget.PROPERTY,  AnnotationTarget.EXPRESSION)
@Retention( AnnotationRetention.SOURCE)
annotation class An

@An
interface A {
    @An
    fun foo(@An a: @An Int = @An 1)
}

@An
interface B {
    @An
    fun foo(@An a: @An Int = @An 2)
}

<!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE("foo; @An() a: @R|An|()  Int = ...;     @An() fun foo(@An() a: @R|An|()  Int = ...): Unit, defined in A    @An() fun foo(@An() a: @R|An|()  Int = ...): Unit, defined in B")!>class AB1<!> : A, B

@An
class AB2 : A, B {
    @An
    override fun foo(<!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES("foo; @An() a: @R|An|()  Int;     @An() fun foo(@An() a: @R|An|()  Int = ...): Unit, defined in A    @An() fun foo(@An() a: @R|An|()  Int = ...): Unit, defined in B")!>@An a: @An Int<!>) {}
}