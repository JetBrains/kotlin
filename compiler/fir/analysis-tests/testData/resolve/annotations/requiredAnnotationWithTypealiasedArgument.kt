// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76746

package testPack

typealias MyTypeAlias = AnnotationTarget

@Target(MyTypeAlias.ANNOTATION_CLASS)
annotation class MyAnnotation

/* GENERATED_FIR_TAGS: annotationDeclaration, typeAliasDeclaration */
