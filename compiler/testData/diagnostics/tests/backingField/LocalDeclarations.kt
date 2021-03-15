// FIR_IDENTICAL
// !LANGUAGE: +RestrictionOfValReassignmentViaBackingField

class Outer {
    val i: Int = 1
        get() {
            class Inner {
                var i: Int = 2
                    get() {
                        field++
                        return field
                    }
                val j: Int = 3
                    get() {
                        <!VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR!>field<!> = 42
                        return field
                    }

                fun innerMember() {
                    <!VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR!>field<!>++
                }
            }
            return field
        }

    val j: Int = 4
        get() {
            fun local() {
                <!VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR!>field<!>++
                field++
            }
            local()
            return field
        }
}
