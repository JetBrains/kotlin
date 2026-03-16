@Suppress("CAST_NEVER_SUCCEEDS_ERROR")
fun box() = if (4 as? Unit != null) "Fail" else "OK"