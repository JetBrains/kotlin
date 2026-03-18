// WITH_STDLIB
// WITH_REFLECT
// TARGET_BACKEND: JVM_IR

// MODULE: lib
// FILE: lib.kt

package a

annotation class NoTarget

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class PropValueField

@Target(AnnotationTarget.PROPERTY)
annotation class PropertyOnly

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ParameterOnly

@Target(AnnotationTarget.FIELD)
annotation class FieldOnly

class Foo(
    @NoTarget
    @PropValueField
    @PropertyOnly
    @ParameterOnly
    @FieldOnly
    var param: Int
)

// MODULE: app(lib)
// FILE: app.kt

package test

import a.Foo
import kotlin.reflect.full.declaredMemberProperties

fun box(): String {
    val clazz = Foo::class

    val parameterAnnotations = clazz.constructors.single().parameters.single().annotations.map { it.annotationClass.simpleName ?: "" }.toSet()
    val propertyAnnotations = clazz.declaredMemberProperties.single().annotations.map { it.annotationClass.simpleName ?: "" }.toSet()
    val fieldAnnotations = Foo::class.java.getDeclaredField("param").annotations.map { it.annotationClass.simpleName ?: "" }.toSet()

    if (parameterAnnotations != setOf("NoTarget", "PropValueField", "ParameterOnly")) return "Parameters:" + parameterAnnotations
    if (propertyAnnotations != setOf("PropertyOnly")) return "Property:" + propertyAnnotations
    if (fieldAnnotations != setOf("FieldOnly")) return "Field:" + fieldAnnotations

    return "OK"
}
