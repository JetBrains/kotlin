// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_TIER_SUGGESTION: D8 dexing error
// FIR_IDENTICAL
// LANGUAGE: -RestrictionOfValReassignmentViaBackingField

val my: Int = 1
    get() {
        <!VAL_REASSIGNMENT_VIA_BACKING_FIELD_WARNING!>field<!>++
        return field
    }
