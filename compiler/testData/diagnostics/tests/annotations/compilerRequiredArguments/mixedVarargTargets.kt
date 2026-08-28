// RUN_PIPELINE_TILL: FRONTEND

package myPack

import kotlin.annotation.AnnotationTarget.CLASS

enum class MyTarget { FUNCTION }

@Target(CLASS, <!ARGUMENT_TYPE_MISMATCH!>MyTarget.<!AMBIGUOUS_ANNOTATION_ARGUMENT!>FUNCTION<!><!>, *arrayOf(AnnotationTarget.PROPERTY))
annotation class MixedVararg

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, enumDeclaration, enumEntry */
