var x: Int = 1
    set(value) {
        field += value
    }

val y: Int = 1
    get() {
        <!VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR!>field<!> += 1
        return 1
    }
