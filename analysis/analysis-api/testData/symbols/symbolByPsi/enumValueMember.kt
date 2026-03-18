// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1

enum class Style(val value: String) {
    SHEET("foo") {
        override val exitAnimation: String
            get() = "bar"
    };

    abstract val exitAnimation: String
}
