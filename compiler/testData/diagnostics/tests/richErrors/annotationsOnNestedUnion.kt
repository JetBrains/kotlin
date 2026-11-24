// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +RichErrors
// FIR_DUMP
@Target(AnnotationTarget.TYPE)
annotation class Anno(val i: Int)

error class E1
error class E2
error class E3

typealias TA = E1 | E3

fun nested(): String | (@Anno(1 + 1) (E1 | E2)) = ""
fun nested2(): (@Anno(1 + 1) (String | E1)) | E2 = ""
fun aliased(): @Anno(2 + 2) TA | E2 = E2()

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, primaryConstructor,
propertyDeclaration, stringLiteral, typeAliasDeclaration */
