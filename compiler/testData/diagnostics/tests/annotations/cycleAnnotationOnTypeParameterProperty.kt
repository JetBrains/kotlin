// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
package myPack

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val number: Int)

val <@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>42.prop<!>) T> T.prop get() = 22

/* GENERATED_FIR_TAGS: annotationDeclaration, getter, integerLiteral, nullableType, primaryConstructor,
propertyDeclaration, propertyWithExtensionReceiver, typeParameter */
