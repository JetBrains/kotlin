// !LANGUAGE: +RestrictionOfValReassignmentViaBackingField

package a

import java.util.HashSet

val a: MutableSet<String>? = null
    get() {
        if (a == null) {
            <!VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR!>field<!> = HashSet()
        }
        return a
    }

class R {
    val b: String? = null
        get() {
            if (b == null) {
                <!VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR!>field<!> = "b"
            }
            return b
        }
}
