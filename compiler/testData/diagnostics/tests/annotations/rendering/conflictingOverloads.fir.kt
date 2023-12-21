// !RENDER_DIAGNOSTICS_MESSAGES

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS,  AnnotationTarget.PROPERTY,  AnnotationTarget.VALUE_PARAMETER)
annotation class An

@An
data class <!CONFLICTING_OVERLOADS("[@An() fun copy(@An() x: @An() Int): @An() Int]")!>A(@An val x: @An Int)<!> {
    <!CONFLICTING_OVERLOADS("[fun copy(@An() x: @An() Int = ...): A]")!>@An
    fun copy(@An x: @An Int)<!> = x
}