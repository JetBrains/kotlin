// RUN_PIPELINE_TILL: FRONTEND
package test

@BadAnnotation(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
object SomeObject

val some = SomeObject

annotation class BadAnnotation(val s: String)

/* GENERATED_FIR_TAGS: annotationDeclaration, integerLiteral, objectDeclaration, primaryConstructor, propertyDeclaration */
