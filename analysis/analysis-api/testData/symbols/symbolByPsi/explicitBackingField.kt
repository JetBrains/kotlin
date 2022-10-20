// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
var p: Int
    field = "test"
    get() = field.length
    set(value) {
        field = value.toString()
    }
