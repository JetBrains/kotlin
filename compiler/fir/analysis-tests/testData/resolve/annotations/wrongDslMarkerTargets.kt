// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-81221
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.TYPE_PARAMETER
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.LOCAL_VARIABLE
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.annotation.AnnotationTarget.TYPE
import kotlin.annotation.AnnotationTarget.EXPRESSION
import kotlin.annotation.AnnotationTarget.FILE
import kotlin.annotation.AnnotationTarget.TYPEALIAS

@DslMarker
public annotation <!DSL_MARKER_WITH_DEFAULT_TARGETS!>class A0<!>

@DslMarker
@Retention(AnnotationRetention.SOURCE)
@Target(
    CLASS,
    <!WRONG_DSL_MARKER_TARGET!>ANNOTATION_CLASS<!>,
    <!WRONG_DSL_MARKER_TARGET!>TYPE_PARAMETER<!>,
    <!WRONG_DSL_MARKER_TARGET!>PROPERTY<!>,
    <!WRONG_DSL_MARKER_TARGET!>FIELD<!>,
    <!WRONG_DSL_MARKER_TARGET!>LOCAL_VARIABLE<!>,
    <!WRONG_DSL_MARKER_TARGET!>VALUE_PARAMETER<!>,
    <!WRONG_DSL_MARKER_TARGET!>CONSTRUCTOR<!>,
    <!WRONG_DSL_MARKER_TARGET!>FUNCTION<!>,
    <!WRONG_DSL_MARKER_TARGET!>PROPERTY_GETTER<!>,
    <!WRONG_DSL_MARKER_TARGET!>PROPERTY_SETTER<!>,
    TYPE,
    <!WRONG_DSL_MARKER_TARGET!>EXPRESSION<!>,
    <!WRONG_DSL_MARKER_TARGET!>FILE<!>,
    TYPEALIAS,
)
public annotation class A1

@DslMarker
@Retention(AnnotationRetention.SOURCE)
@Target(
    allowedTargets = [
        AnnotationTarget.CLASS,
        <!WRONG_DSL_MARKER_TARGET!>AnnotationTarget.ANNOTATION_CLASS<!>,
        <!WRONG_DSL_MARKER_TARGET!>AnnotationTarget.TYPE_PARAMETER<!>,
        <!WRONG_DSL_MARKER_TARGET!>AnnotationTarget.PROPERTY<!>,
        <!WRONG_DSL_MARKER_TARGET!>AnnotationTarget.FIELD<!>,
        <!WRONG_DSL_MARKER_TARGET!>AnnotationTarget.LOCAL_VARIABLE<!>,
        <!WRONG_DSL_MARKER_TARGET!>AnnotationTarget.VALUE_PARAMETER<!>,
        <!WRONG_DSL_MARKER_TARGET!>AnnotationTarget.CONSTRUCTOR<!>,
        <!WRONG_DSL_MARKER_TARGET!>AnnotationTarget.FUNCTION<!>,
        <!WRONG_DSL_MARKER_TARGET!>AnnotationTarget.PROPERTY_GETTER<!>,
        <!WRONG_DSL_MARKER_TARGET!>AnnotationTarget.PROPERTY_SETTER<!>,
        AnnotationTarget.TYPE,
        <!WRONG_DSL_MARKER_TARGET!>AnnotationTarget.EXPRESSION<!>,
        <!WRONG_DSL_MARKER_TARGET!>AnnotationTarget.FILE<!>,
        AnnotationTarget.TYPEALIAS,
    ]
)
public annotation class A2

@DslMarker
@Retention(AnnotationRetention.SOURCE)
@Target(
    CLASS,
    TYPE,
)
public annotation class A3

@DslMarker
@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.TYPE,
)
public annotation class A4

/* GENERATED_FIR_TAGS: annotationDeclaration */
