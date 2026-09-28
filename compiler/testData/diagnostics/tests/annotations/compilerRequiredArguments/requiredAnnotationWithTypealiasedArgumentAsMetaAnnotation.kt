// RUN_PIPELINE_TILL: CODEGEN
// FIR_DUMP

package testPack

typealias MyTypeAlias = AnnotationTarget

@Target(MyTypeAlias.FIELD)
annotation class MyAnnotation

@MyAnnotation
val property = 0

/* GENERATED_FIR_TAGS: annotationDeclaration, integerLiteral, propertyDeclaration, typeAliasDeclaration */
