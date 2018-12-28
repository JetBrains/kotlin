// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

const val MaxUI = UInt.MAX_VALUE
const val MinUI = UInt.MIN_VALUE

const val MaxUL = ULong.MAX_VALUE
const val MinUL = ULong.MIN_VALUE

val M = MaxUI.toULong()

val range1 = 1u .. 6u
fun testSimpleUIntLoop() {
    var s = 0
    for (i in range1) {
        s = s*10 + i.toInt()
    }
    if (s != 123456) throw AssertionError("$s")
}

val range2 = 6u .. 1u
fun testEmptyUIntLoop() {
    var s = 0
    for (i in range2) {
        s = s*10 + i.toInt()
    }
    if (s != 0) throw AssertionError("$s")
}

val range3 = 1UL .. 6UL
fun testSimpleULongLoop() {
    var s = 0
    for (i in range3) {
        s = s*10 + i.toInt()
    }
    if (s != 123456) throw AssertionError("$s")
}

val range4 = 6UL .. 1UL
fun testEmptyULongLoop() {
    var s = 0
    for (i in range4) {
        s = s*10 + i.toInt()
    }
    if (s != 0) throw AssertionError("$s")
}

val range5 = M+1UL..M+6UL
fun testULongLoop() {
    var s = 0
    for (i in range5) {
        s = s*10 + (i-M).toInt()
    }
    if (s != 123456) throw AssertionError("$s")
}

val range6 = M+6UL..M+1UL
fun testEmptyULongLoop2() {
    var s = 0
    for (i in range6) {
        s = s*10 + (i-M).toInt()
    }
    if (s != 0) throw AssertionError("$s")
}

val range7 = MaxUI..MinUI
fun testMaxUItoMinUI() {
    val xs = ArrayList<UInt>()
    for (i in range7) {
        xs.add(i)
        if (xs.size > 23) break
    }
    if (xs.size > 0) {
        throw AssertionError("Wrong elements for MaxUI..MinUI: $xs")
    }
}

val range8 = MaxUL..MinUL
fun testMaxULtoMinUL() {
    val xs = ArrayList<ULong>()
    for (i in range8) {
        xs.add(i)
        if (xs.size > 23) break
    }
    if (xs.size > 0) {
        throw AssertionError("Wrong elements for MaxUI..MinUI: $xs")
    }
}

fun box(): String {
    testSimpleUIntLoop()
    testEmptyUIntLoop()
    testSimpleULongLoop()
    testEmptyULongLoop()
    testULongLoop()
    testEmptyULongLoop2()
    testMaxUItoMinUI()
    testMaxULtoMinUL()

    return "OK"
}