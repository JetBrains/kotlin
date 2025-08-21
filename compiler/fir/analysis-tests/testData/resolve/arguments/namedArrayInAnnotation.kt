// RUN_PIPELINE_TILL: BACKEND
annotation class Ann(vararg val strings: String)

@Ann(strings = ["hello"])
class A

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, collectionLiteral, outProjection, primaryConstructor,
propertyDeclaration, stringLiteral, vararg */
