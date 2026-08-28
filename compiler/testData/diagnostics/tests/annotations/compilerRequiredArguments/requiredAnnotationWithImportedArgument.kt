// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-75844

import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS

@Target(ANNOTATION_CLASS)
annotation class MyAnnotation

/* GENERATED_FIR_TAGS: annotationDeclaration */
