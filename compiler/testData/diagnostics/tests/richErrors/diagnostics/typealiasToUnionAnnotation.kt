// RUN_PIPELINE_TILL: FRONTEND
error class E

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER)
annotation class Ann

typealias AnnR = Ann | E
typealias A = Ann

@<!NOT_AN_ANNOTATION_CLASS!>AnnR<!> fun f() {}
@<!NOT_AN_ANNOTATION_CLASS!>AnnR<!> class C
fun g(@<!NOT_AN_ANNOTATION_CLASS!>AnnR<!> x: Int) {}
@A fun ok() {}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, propertyDeclaration, stringLiteral,
typeAliasDeclaration */
