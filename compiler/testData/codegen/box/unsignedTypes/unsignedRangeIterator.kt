// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// KOTLIN_CONFIGURATION_FLAGS: +JVM.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME

fun testUIntRangeForEach() {
    var s = 0
    (1u .. 5u).forEach {
        s = s * 10 + it.toInt()
    }
    if (s != 12345) throw AssertionError("$s")
}

fun testUIntProgressionForEach() {
    var s = 0
    (5u downTo 1u).forEach {
        s = s * 10 + it.toInt()
    }
    if (s != 54321) throw AssertionError("$s")
}

fun testULongRangeForEach() {
    var s = 0
    (1UL .. 5UL).forEach {
        s = s * 10 + it.toInt()
    }
    if (s != 12345) throw AssertionError("$s")
}

fun testULongProgressionForEach() {
    var s = 0
    (5UL downTo 1UL).forEach {
        s = s * 10 + it.toInt()
    }
    if (s != 54321) throw AssertionError("$s")
}


fun box(): String {
    testUIntRangeForEach()
    testUIntProgressionForEach()
    testULongRangeForEach()
    testULongProgressionForEach()

    return "OK"
}