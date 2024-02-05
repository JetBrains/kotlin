// FIR_IDENTICAL
val n: Any? = null

enum class En(val x: String?) {
    ENTRY(n?.toString())
}
