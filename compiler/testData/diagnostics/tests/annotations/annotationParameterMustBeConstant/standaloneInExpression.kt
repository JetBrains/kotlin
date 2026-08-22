// RUN_PIPELINE_TILL: FRONTEND
annotation class AnnE(val i: String)

enum class MyEnum {
    A
}

@AnnE("1" + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>MyEnum.A<!>)
class Test

@AnnE("1" + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>MyEnum::class<!>)
class Test2

@AnnE("1" + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>AnnE("23")<!>)
class Test3

@AnnE("1" + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>arrayOf("23", "34")<!>)
class Test4

/* GENERATED_FIR_TAGS: additiveExpression, annotationDeclaration, classDeclaration, classReference, collectionLiteral,
enumDeclaration, enumEntry, primaryConstructor, propertyDeclaration, stringLiteral */
