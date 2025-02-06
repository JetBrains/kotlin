// LL_FIR_DIVERGENCE
// Apparently, LL(Reversed)DiagnosticsFe10TestGenerated does NOT run FE 1.0 despite its name,
// it runs FIR checkers as well (not sure why these tests are even generated for LL).
// However, it does not call FirCompilerRequiredAnnotationsResolveTransformer.transformFile to resolve annotations,
// and therefore @MustUseReturnValue annotations are not placed automatically.
// LL_FIR_DIVERGENCE

// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FIR_DUMP

@file:NonCompilerAnnotation

@Target(AnnotationTarget.FILE, AnnotationTarget.FUNCTION)
annotation class NonCompilerAnnotation

@NonCompilerAnnotation
fun foo(): String = ""

fun main() {
    foo()
}
