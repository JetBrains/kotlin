// FIR_IDENTICAL
// !LANGUAGE: +ProhibitUseSiteGetTargetAnnotations

@Target(AnnotationTarget.PROPERTY) annotation class Annotation

enum class Foo {
    <!INAPPLICABLE_TARGET_ON_PROPERTY!>@property:Annotation<!>
    Entry
}
