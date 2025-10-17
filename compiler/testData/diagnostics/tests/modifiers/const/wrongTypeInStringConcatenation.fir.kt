// RUN_PIPELINE_TILL: FRONTEND

enum class EnumClass {
    VALUE
}

const val enumStringConcat = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"${EnumClass.VALUE}"<!>
const val arrayLiteralStringConcat = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"${<!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>['1']<!>}"<!>

annotation class Anno(val str1: String, val str2: String)
@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"${EnumClass.VALUE}"<!>, <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"${['1']}"<!>)
class MyClass

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, collectionLiteral, const, enumDeclaration, enumEntry,
primaryConstructor, propertyDeclaration */
