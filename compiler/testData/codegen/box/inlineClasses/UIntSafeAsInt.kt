// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun testUIntSafeAsInt(x: UInt) = x as? Int

fun box(): String = if (testUIntSafeAsInt(1U) != null) "fail" else "OK"