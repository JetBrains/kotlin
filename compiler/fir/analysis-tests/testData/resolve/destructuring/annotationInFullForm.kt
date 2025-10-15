// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring
// COMPARE_WITH_LIGHT_TREE
@Target(AnnotationTarget.LOCAL_VARIABLE)
annotation class Ann

data class Person(val fullName: String)

fun annSimpleNegative(person: Person) {
    <!WRAPPED_LHS_IN_ASSIGNMENT_WARNING!>(<!EXPRESSION_EXPECTED{PSI}, VARIABLE_EXPECTED!>@Ann val <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER{PSI}!>fullName<!><!>)<!> = person
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, data, functionDeclaration, primaryConstructor,
propertyDeclaration */
