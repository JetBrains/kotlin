// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_PHASE_VERIFICATION: invalid code inside annotations

@file:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, ARGUMENT_TYPE_MISMATCH!>fun(): Int {
    val s: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> "str"
    return s
}<!>)

package one

@Target(AnnotationTarget.FILE)
annotation class Anno(val s: String)

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetFile, anonymousFunction, localProperty,
primaryConstructor, propertyDeclaration, stringLiteral */
