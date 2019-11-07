var x: Int = 1
    set(value) {
        field += value
    }

val y: Int = 1
    get() {
        <!VARIABLE_EXPECTED!>field<!> += 1
        return 1
    }
