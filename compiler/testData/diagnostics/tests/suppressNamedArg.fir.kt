// ISSUE: KT-62146

@Deprecated("This is deprecated", level = DeprecationLevel.WARNING)
fun deprecated() = 1

@Suppress("DEPRECATION")
fun main() = deprecated()

@Suppress(names = ["DEPRECATION"])
fun plain() = <!DEPRECATION!>deprecated<!>()

@Suppress(names = arrayOf("DEPRECATION"))
fun brain() = <!DEPRECATION!>deprecated<!>()
