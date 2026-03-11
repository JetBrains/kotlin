// WITH_STDLIB
// WITH_REFLECT
// TARGET_BACKEND: JVM_IR
// FIR_DUMP
// DUMP_IR

import kotlin.reflect.full.declaredMemberProperties
import kotlin.annotation.AnnotationTarget.*

annotation class NoTarget

@Target(kotlin.annotation.AnnotationTarget.PROPERTY, VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class PropValueField

@Target(allowedTargets = [AnnotationTarget.PROPERTY])
annotation class PropertyOnly

@Target(allowedTargets = arrayOf(AnnotationTarget.VALUE_PARAMETER))
annotation class ParameterOnly

@Target(allowedTargets = *arrayOf(AnnotationTarget.FIELD))
annotation class FieldOnly

@Target(*[AnnotationTarget.PROPERTY])
annotation class PropertyOnly2

class Foo(
    @NoTarget
    @PropValueField
    @PropertyOnly
    @PropertyOnly2
    @ParameterOnly
    @FieldOnly
    var param: Int
)

fun box(): String {
    val clazz = Foo::class

    val parameterAnnotations = clazz.constructors.single().parameters.single().annotations.map { it.annotationClass.simpleName ?: "" }.toSet()
    val propertyAnnotations = clazz.declaredMemberProperties.single().annotations.map { it.annotationClass.simpleName ?: "" }.toSet()
    val fieldAnnotations = Foo::class.java.getDeclaredField("param").annotations.map { it.annotationClass.simpleName ?: "" }.toSet()

    if (parameterAnnotations != setOf("NoTarget", "PropValueField", "ParameterOnly")) return "Parameters:" + parameterAnnotations.joinToString()
    if (propertyAnnotations != setOf("PropertyOnly", "PropertyOnly2")) return "Property:" + propertyAnnotations.joinToString()
    if (fieldAnnotations != setOf("FieldOnly")) return "Field:" + fieldAnnotations.joinToString()

    return "OK"
}
