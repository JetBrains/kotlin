// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
annotation class Ann(val prop: String)

val annCtorRef = ::<!CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR!>Ann<!>
val annClassRef = Ann::class
val annPropRef = Ann::prop

/* GENERATED_FIR_TAGS: annotationDeclaration, callableReference, classReference, primaryConstructor, propertyDeclaration */
