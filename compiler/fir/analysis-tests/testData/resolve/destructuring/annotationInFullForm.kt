// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring

@Target(AnnotationTarget.LOCAL_VARIABLE)
annotation class Ann

data class Person(val fullName: String)

fun annSimpleNegative(person: Person) {
    <!WRAPPED_LHS_IN_ASSIGNMENT_WARNING!>(<!EXPRESSION_EXPECTED, VARIABLE_EXPECTED!>@Ann val <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>fullName<!><!>)<!> = person
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, data, functionDeclaration, primaryConstructor,
propertyDeclaration */
