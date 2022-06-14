// FIR_IDENTICAL
// LANGUAGE: +ProhibitCyclesInAnnotations
// ISSUE: KT-52742

annotation class AnnotationWithArray(
    val array: Array<AnnotationWithArray>
)

annotation class AnnotationWithVararg(
    vararg val args: AnnotationWithVararg
)
