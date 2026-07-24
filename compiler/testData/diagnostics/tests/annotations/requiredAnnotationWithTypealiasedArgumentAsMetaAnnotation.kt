// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP

package testPack

typealias MyTypeAlias = AnnotationTarget

@Target(MyTypeAlias.FIELD)
annotation class MyAnnotation

@MyAnnotation
val property = 0

/* GENERATED_FIR_TAGS: annotationDeclaration, integerLiteral, propertyDeclaration, typeAliasDeclaration */
