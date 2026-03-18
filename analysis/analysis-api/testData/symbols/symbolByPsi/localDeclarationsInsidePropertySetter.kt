// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
var x: Int = 5
    set(value) {
        val localProp = 10
        fun localFun() {}
        class LocalClass {}
        typealias LocalTypealias = String
        field = value
    }