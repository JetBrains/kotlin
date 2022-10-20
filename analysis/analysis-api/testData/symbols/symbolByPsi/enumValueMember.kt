// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
enum class Style(val value: String) {
    SHEET("foo") {
        override val exitAnimation: String
            get() = "bar"
    };

    abstract val exitAnimation: String
}
