// !GENERATE_PROPERTY_ANNOTATIONS_METHODS
@Target(AnnotationTarget.PROPERTY)
annotation class PropertyAnnotation

@Target(AnnotationTarget.FIELD)
annotation class FieldAnnotation

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ParameterAnnotation

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class PropertyOrFieldAnnotation

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
annotation class PropertyOrParameterAnnotation

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
annotation class ParameterOrFieldAnnotation

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
annotation class UniversalAnnotation

annotation class AnotherUniversalAnnotation


class MyClass(
    @PropertyAnnotation @FieldAnnotation @ParameterAnnotation @UniversalAnnotation @AnotherUniversalAnnotation val x1: Int,
    @PropertyOrFieldAnnotation val x2: Int,
    @PropertyOrParameterAnnotation val x3: Int,
    @ParameterOrFieldAnnotation val x4: Int,
    @property:UniversalAnnotation @field:AnotherUniversalAnnotation val x5: Int,
    @field:UniversalAnnotation @param:AnotherUniversalAnnotation val x6: Int,
    @param:UniversalAnnotation @property:AnotherUniversalAnnotation val x7: Int
)
