// WITH_STDLIB

fun isImportedByDefault(c: String?, x: Set<Int>) = c?.let { it.toInt() } in x

fun box(): String = "OK"
