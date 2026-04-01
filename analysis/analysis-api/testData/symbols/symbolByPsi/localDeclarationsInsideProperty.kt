// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
val x = when (true) {
    true -> {
        val localProp = 10
        fun localFun() {}
        class LocalClass {}
        typealias LocalTypealias = String
        1
    }
    else -> 2
}
