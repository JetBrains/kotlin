// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib tests compile against JDK 17 instead of the mock JDK.
val n: Any? = null

enum class En(val x: String?) {
    ENTRY(n?.toString())
}
