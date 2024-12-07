// This test checks that we don't generate target TYPE_USE if `-Xno-new-java-annotation-targets` is used.
// It's important that this test depends on _full JDK_, which has ElementType.TYPE_USE, to check that filtering based on
// the compiler argument is taking place.

// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// NO_NEW_JAVA_ANNOTATION_TARGETS
// FULL_JDK
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
