// RUN_PIPELINE_TILL: FRONTEND
class B {}

val b : B<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!> = 1

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, propertyDeclaration */
