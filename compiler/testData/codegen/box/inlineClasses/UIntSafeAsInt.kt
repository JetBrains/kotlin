// WITH_STDLIB

@Suppress("CAST_NEVER_SUCCEEDS_ERROR")
fun testUIntSafeAsInt(x: UInt) = x as? Int

fun box(): String = if (testUIntSafeAsInt(1U) != null) "fail" else "OK"