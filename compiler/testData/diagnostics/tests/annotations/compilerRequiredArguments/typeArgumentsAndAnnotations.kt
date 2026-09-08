// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-86978

import kotlin.annotation.AnnotationTarget.FIELD

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Marker

@Target(@Marker @<!UNRESOLVED_REFERENCE!>Unresolved<!> AnnotationTarget.FIELD)
annotation class Annotated1

@Target(@Marker @<!UNRESOLVED_REFERENCE!>Unresolved<!> kotlin.annotation.AnnotationTarget.FIELD)
annotation class Annotated2

@Target((@Marker @<!UNRESOLVED_REFERENCE!>Unresolved<!> AnnotationTarget).FIELD)
annotation class Annotated3

@Target((@Marker @<!UNRESOLVED_REFERENCE!>Unresolved<!> kotlin.annotation.AnnotationTarget).FIELD)
annotation class Annotated4

@Target(<!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>(kotlin.annotation)<!>.AnnotationTarget.FIELD)
annotation class Parenthesized

@Target((@Marker @<!UNRESOLVED_REFERENCE!>Unresolved<!> <!UNRESOLVED_REFERENCE!>kotlin<!>).annotation.AnnotationTarget.FIELD)
annotation class Annotated5

@Target(@Marker @<!UNRESOLVED_REFERENCE!>Unresolved<!> FIELD)
annotation class Annotated6

@Target(<!UNRESOLVED_REFERENCE!>kotlin<!><Int>.annotation.AnnotationTarget.FIELD)
annotation class TypeArgumentOnFirstPart

@Target(kotlin.<!UNRESOLVED_REFERENCE!>annotation<!><Int>.AnnotationTarget.FIELD)
annotation class TypeArgumentOnSecondPart

class C {
    @Annotated1
    @Annotated2
    @Annotated3
    @Annotated4
    @Parenthesized
    <!WRONG_ANNOTATION_TARGET!>@Annotated5<!>
    @Annotated6
    <!WRONG_ANNOTATION_TARGET!>@TypeArgumentOnFirstPart<!>
    <!WRONG_ANNOTATION_TARGET!>@TypeArgumentOnSecondPart<!>
    val field = 1
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, integerLiteral, propertyDeclaration */
