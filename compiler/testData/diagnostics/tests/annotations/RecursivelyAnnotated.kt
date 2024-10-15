// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// Class CAN be recursively annotated
@RecursivelyAnnotated(1)
annotation class RecursivelyAnnotated(val x: Int)