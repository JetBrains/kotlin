// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +ProhibitUseSiteGetTargetAnnotations

@Target(AnnotationTarget.PROPERTY) annotation class Annotation

enum class Foo {
    <!INAPPLICABLE_TARGET_ON_PROPERTY!>@property:Annotation<!>
    Entry
}

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetProperty, enumDeclaration, enumEntry */
