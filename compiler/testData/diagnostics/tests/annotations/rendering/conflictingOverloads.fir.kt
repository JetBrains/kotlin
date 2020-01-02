// !RENDER_DIAGNOSTICS_MESSAGES

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS,  AnnotationTarget.PROPERTY,  AnnotationTarget.VALUE_PARAMETER)
annotation class An

@An
data class A(@An val x: @An Int) {
    @An
    fun copy(@An x: @An Int) = x
}