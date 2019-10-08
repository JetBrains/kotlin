// !RENDER_DIAGNOSTICS_MESSAGES

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS,  AnnotationTarget.PROPERTY,  AnnotationTarget.VALUE_PARAMETER)
annotation class An

@An
data class A(@An val x: @An Int) {
    <!CONFLICTING_OVERLOADS("public final fun copy(x: Int): Int defined in A")!>@An
    fun copy(@An x: @An Int)<!> = x
}