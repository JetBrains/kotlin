// DUMP_IR_DIFFERENCE: JKLIB
val n: Any? = null

enum class En(val x: String?) {
    ENTRY(n?.toString())
}
