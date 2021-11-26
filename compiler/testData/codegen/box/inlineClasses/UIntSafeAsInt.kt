// WITH_STDLIB

fun testUIntSafeAsInt(x: UInt) = x as? Int

fun box(): String = if (testUIntSafeAsInt(1U) != null) "fail" else "OK"