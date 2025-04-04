// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

val my: Int = 1
    get() {
        <!VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR!>field<!>++
        return field
    }