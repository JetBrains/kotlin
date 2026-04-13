// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ProhibitUseSiteGetTargetAnnotations

@Target(AnnotationTarget.PROPERTY) annotation class Annotation

enum class Foo {
    <!INAPPLICABLE_TARGET_ON_PROPERTY_WARNING!>@property:Annotation<!>
    Entry
}

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetProperty, enumDeclaration, enumEntry */
