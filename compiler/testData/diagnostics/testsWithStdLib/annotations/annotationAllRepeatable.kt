// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-85005
// FIR_DUMP

annotation class A

class B(
    @param:A
    <!REPEATED_ANNOTATION!>@all:A<!>
    val x: Int,

    // Two errors: parameter, property
    @A
    <!REPEATED_ANNOTATION, REPEATED_ANNOTATION!>@all:A<!>
    val y: Int,

    // Two errors: parameter, property
    @all:A
    <!REPEATED_ANNOTATION, REPEATED_ANNOTATION!>@A<!>
    val z: Int,

    @get:A
    <!REPEATED_ANNOTATION!>@all:A<!>
    val w: Int,

    // No error here as @all: isn't applied to setter
    @all:A
    @set:A
    var v: Int,

    <!REPEATED_ANNOTATION!>@all:A<!>
    @setparam:A
    var t: Int,

    @field:A
    <!REPEATED_ANNOTATION!>@all:A<!>
    val u: Int,

    @property:A
    <!REPEATED_ANNOTATION!>@all:A<!>
    val s: Int,

    // Four errors: parameter, property, getter, field
    @all:A
    <!REPEATED_ANNOTATION, REPEATED_ANNOTATION, REPEATED_ANNOTATION, REPEATED_ANNOTATION!>@all:A<!>
    val r: Int,
)

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetAll, annotationUseSiteTargetField,
annotationUseSiteTargetParam, annotationUseSiteTargetProperty, annotationUseSiteTargetPropertyGetter,
annotationUseSiteTargetPropertySetter, annotationUseSiteTargetSetterParameter, classDeclaration, primaryConstructor,
propertyDeclaration */
