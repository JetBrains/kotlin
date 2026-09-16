// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-76746

package testPack

import testPack.MyTypeAlias.*

typealias MyTypeAlias = AnnotationTarget

@Target(ANNOTATION_CLASS)
annotation class MyAnnotation

/* GENERATED_FIR_TAGS: annotationDeclaration, typeAliasDeclaration */
