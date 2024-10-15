// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// check no error when regular function and extension function have same name

package extensionAndRegular

fun who() = 1

fun Int.who() = 1
