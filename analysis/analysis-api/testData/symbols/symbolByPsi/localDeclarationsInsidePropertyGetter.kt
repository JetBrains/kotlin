// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
val x: Int = 5
    get() {
        val localProp = 10
        fun localFun() {}
        class LocalClass {}
        typealias LocalTypealias = String
        return field
    }