@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ParameterAnnotation(val i: Int)

class Sub(
    @ParameterAnnotation(konstant)
    var pr<caret>op: Int,
)

const val konstant = 0