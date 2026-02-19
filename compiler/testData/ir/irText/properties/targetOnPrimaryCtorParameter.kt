// FIR_IDENTICAL
import kotlin.annotation.AnnotationTarget.*

annotation class NoTarget

@Target(kotlin.annotation.AnnotationTarget.PROPERTY, VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class PropValueField

@Target(allowedTargets = [AnnotationTarget.PROPERTY])
annotation class PropertyOnly

@Target(allowedTargets = arrayOf(AnnotationTarget.VALUE_PARAMETER))
annotation class ParameterOnly

@Target(*[AnnotationTarget.PROPERTY])
annotation class PropertyOnly2

class Foo(
    @NoTarget
    @PropValueField
    @PropertyOnly
    @PropertyOnly2
    @ParameterOnly
    var param: Int
)