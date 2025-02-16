// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -DEPRECATED_JAVA_ANNOTATION
// RENDER_DIAGNOSTICS_FULL_TEXT

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
<!INCOMPATIBLE_ANNOTATION_TARGETS("target 'METHOD'; targets 'PROPERTY_GETTER', 'PROPERTY_SETTER'")!>@java.lang.annotation.Target(java.lang.annotation.ElementType.PARAMETER)<!>
annotation class A

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@java.lang.annotation.Target(java.lang.annotation.ElementType.PARAMETER, java.lang.annotation.ElementType.METHOD)
annotation class B

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@java.lang.annotation.Target(java.lang.annotation.ElementType.PARAMETER, java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.FIELD)
annotation class C