// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-85005
// FIR_DUMP

annotation class A

class B(
    @param:A
    @all:A
    val x: Int,

    @A
    @all:A
    val y: Int,

    @all:A
    @A
    val z: Int,

    @get:A
    <!REPEATED_ANNOTATION!>@all:A<!>
    val w: Int,

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
    @all:A
    val s: Int,

    @all:A
    <!REPEATED_ANNOTATION, REPEATED_ANNOTATION, REPEATED_ANNOTATION, REPEATED_ANNOTATION!>@all:A<!>
    val r: Int,
)

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetAll, annotationUseSiteTargetField,
annotationUseSiteTargetParam, annotationUseSiteTargetProperty, annotationUseSiteTargetPropertyGetter,
annotationUseSiteTargetPropertySetter, annotationUseSiteTargetSetterParameter, classDeclaration, primaryConstructor,
propertyDeclaration */
