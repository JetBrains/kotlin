// This test checks that we don't generate target TYPE_USE if there's JDK 1.6 in the classpath.
// It's important that this test depends on _mock JDK_, which doesn't have ElementType.TYPE_USE.

// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// WITH_STDLIB

import kotlin.annotation.AnnotationTarget.*

@Target(
    CLASS,
    ANNOTATION_CLASS,
    TYPE_PARAMETER,
    PROPERTY,
    FIELD,
    LOCAL_VARIABLE,
    VALUE_PARAMETER,
    CONSTRUCTOR,
    FUNCTION,
    PROPERTY_GETTER,
    PROPERTY_SETTER,
    TYPE,
    EXPRESSION,
    FILE,
    TYPEALIAS,
)
@Retention(AnnotationRetention.SOURCE)
annotation class A

fun box(): String {
    val targets = A::class.java.getAnnotation(java.lang.annotation.Target::class.java).value
    if (targets.toList().toString() != "[TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE]")
        return "Fail: Java annotation target list should not contain TYPE_USE/TYPE_PARAMETER: ${targets.toList()}"

    return "OK"
}
