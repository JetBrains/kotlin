// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
// Class constructor parameter type CAN be recursively annotated
@Target(AnnotationTarget.TYPE)
annotation class RecursivelyAnnotated(val x: @RecursivelyAnnotated(1) Int)
