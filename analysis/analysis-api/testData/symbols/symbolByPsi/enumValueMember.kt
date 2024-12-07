// DO_NOT_CHECK_SYMBOL_RESTORE_K1

enum class Style(val value: String) {
    SHEET("foo") {
        override val exitAnimation: String
            get() = "bar"
    };

    abstract val exitAnimation: String
}
