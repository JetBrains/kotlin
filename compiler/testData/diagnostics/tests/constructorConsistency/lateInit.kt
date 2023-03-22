// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
class WithLateInit {
    lateinit var x: String

    fun init(xx: String) {
        x = xx
    }

    init {
        // This is obviously a bug,
        // but with lateinit we explicitly want it to occur in runtime
        use()
    }

    fun use() = x
}