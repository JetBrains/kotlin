// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
var my: Int = 0
    get() = -field
    set(arg) {
        field = arg
    }
