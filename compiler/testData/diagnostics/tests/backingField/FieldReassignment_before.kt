// !LANGUAGE: -RestrictionOfValReassignmentViaBackingField

val my: Int = 1
    get() {
        <!VAL_REASSIGNMENT_VIA_BACKING_FIELD!>field<!>++
        return field
    }