// FIR_IDENTICAL
// ISSUE: KT-62146

@Deprecated("This is deprecated", level = DeprecationLevel.WARNING)
fun deprecated() = 1

@Suppress("DEPRECATION")
fun main() = deprecated()

@Suppress(names = ["DEPRECATION"])
fun plain() = deprecated()

@Suppress(names = arrayOf("DEPRECATION"))
fun brain() = deprecated()
