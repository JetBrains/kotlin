// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
var x: Int = 5
    set(value) {
        val localProp = 10
        fun localFun() {}
        class LocalClass {}
        typealias LocalTypealias = String
        field = value
    }