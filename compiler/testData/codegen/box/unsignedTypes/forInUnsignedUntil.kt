// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

const val MaxUI = UInt.MAX_VALUE
const val MinUI = UInt.MIN_VALUE

const val MaxUL = ULong.MAX_VALUE
const val MinUL = ULong.MIN_VALUE

val M = MaxUI.toULong()
val N = Int.MAX_VALUE.toUInt()

fun testSimpleUIntLoop() {
    var s = 0
    for (i in 1u until 6u) {
        s = s*10 + i.toInt()
    }
    if (s != 12345) throw AssertionError("$s")
}

fun testEmptyUIntLoop() {
    var s = 0
    for (i in 6u until 1u) {
        s = s*10 + i.toInt()
    }
    if (s != 0) throw AssertionError("$s")
}

fun testSimpleULongLoop() {
    var s = 0
    for (i in 1UL until 6UL) {
        s = s*10 + i.toInt()
    }
    if (s != 12345) throw AssertionError("$s")
}

fun testEmptyULongLoop() {
    var s = 0
    for (i in 6UL until 1UL) {
        s = s*10 + i.toInt()
    }
    if (s != 0) throw AssertionError("$s")
}

fun testULongLoop() {
    var s = 0
    for (i in M+1UL until M+6UL) {
        s = s*10 + (i-M).toInt()
    }
    if (s != 12345) throw AssertionError("$s")
}

fun testEmptyULongLoop2() {
    var s = 0
    for (i in M+6UL until M+1UL) {
        s = s*10 + (i-M).toInt()
    }
    if (s != 0) throw AssertionError("$s")
}

fun testMaxUItoMinUI() {
    val xs = ArrayList<UInt>()
    for (i in MaxUI until MinUI) {
        xs.add(i)
        if (xs.size > 23) break
    }
    if (xs.size > 0) {
        throw AssertionError("Wrong elements for MaxUI..MinUI: $xs")
    }
}

fun testMaxULtoMinUL() {
    val xs = ArrayList<ULong>()
    for (i in MaxUL until MinUL) {
        xs.add(i)
        if (xs.size > 23) break
    }
    if (xs.size > 0) {
        throw AssertionError("Wrong elements for MaxUI..MinUI: $xs")
    }
}

fun testWrappingUIntLoop() {
    val NA = N - 1u
    val NB = N + 1u
    val xs = ArrayList<UInt>()
    for (i in NA until NB) {
        xs.add(i)
        if (xs.size > 3) break
    }
    if (xs != listOf(NA, N)) throw AssertionError("$xs")
}

fun testWrappingULongLoop() {
    val MA = M - 1UL
    val MB = M + 1UL
    val xs = ArrayList<ULong>()
    for (i in MA until MB) {
        xs.add(i)
        if (xs.size > 3) break
    }
    if (xs != listOf(MA, M)) throw AssertionError("$xs")
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
    testWrappingUIntLoop()
    testWrappingULongLoop()

    return "OK"
}