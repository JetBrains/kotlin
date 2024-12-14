// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_MESSAGES
// LATEST_LV_DIFFERENCE

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS,  AnnotationTarget.PROPERTY,  AnnotationTarget.VALUE_PARAMETER)
annotation class An

@An
data class <!CONFLICTING_OVERLOADS("fun copy(x: @An() Int): @An() Int")!>A(@An val x: @An Int)<!> {
    @An
    <!CONFLICTING_OVERLOADS("fun copy(x: @An() Int = ...): A")!>fun copy(@An x: @An Int)<!> = x
}