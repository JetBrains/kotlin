// IGNORE_BACKEND_FIR: ANY
val n: Any? = null

enum class En(val x: String?) {
    ENTRY(n?.toString())
}