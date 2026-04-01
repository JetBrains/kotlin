// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
val x: Int = 5
    get() {
        val localProp = 10
        fun localFun() {}
        class LocalClass {}
        typealias LocalTypealias = String
        return field
    }