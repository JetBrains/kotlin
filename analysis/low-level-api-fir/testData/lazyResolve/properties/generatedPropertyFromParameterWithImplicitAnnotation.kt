// MEMBER_NAME_FILTER: prop

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ParameterAnnotation(val i: Int)

class S<caret>ub(
    @ParameterAnnotation(konstant)
    var prop: Int,
)

const val konstant = 0