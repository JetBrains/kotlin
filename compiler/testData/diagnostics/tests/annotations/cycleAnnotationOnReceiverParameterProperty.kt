// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
package myPack

annotation class Anno(val number: Int)

val @receiver:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>42.prop<!>) Int.prop get() = 22

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetReceiver, getter, integerLiteral,
primaryConstructor, propertyDeclaration, propertyWithExtensionReceiver */
