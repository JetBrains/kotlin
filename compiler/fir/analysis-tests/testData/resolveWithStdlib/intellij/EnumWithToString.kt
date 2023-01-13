// FIR_DISABLE_LAZY_RESOLVE_CHECKS
enum class Some {
    ENTRY {
        override fun toString(): String = "Entry"
    };

    override fun toString(): String = "Some"
}
