// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
package customGetValGlobal
    val zz = 1
        get() = field * 2
