// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LATEST_LV_DIFFERENCE
// Class constructor parameter CAN be recursively annotated
annotation class RecursivelyAnnotated(@RecursivelyAnnotated(1) val x: Int)
