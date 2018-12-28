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
    for (i in 6u downTo 1u) {
        s = s*10 + i.toInt()
    }
    if (s != 654321) throw AssertionError("$s")
}

fun testEmptyUIntLoop() {
    var s = 0
    for (i in 1u downTo 6u) {
        s = s*10 + i.toInt()
    }
    if (s != 0) throw AssertionError("$s")
}

fun testSimpleULongLoop() {
    var s = 0
    for (i in 6UL downTo 1UL) {
        s = s*10 + i.toInt()
    }
    if (s != 654321) throw AssertionError("$s")
}

fun testEmptyULongLoop() {
    var s = 0
    for (i in 1UL downTo 6UL) {
        s = s*10 + i.toInt()
    }
    if (s != 0) throw AssertionError("$s")
}

fun testULongLoop() {
    var s = 0
    for (i in M+6UL downTo M+1UL) {
        s = s*10 + (i-M).toInt()
    }
    if (s != 654321) throw AssertionError("$s")
}

fun testEmptyULongLoop2() {
    var s = 0
    for (i in M+1UL downTo M+6UL) {
        s = s*10 + (i-M).toInt()
    }
    if (s != 0) throw AssertionError("$s")
}

fun testMaxUIdownToMinUI() {
    val xs = ArrayList<UInt>()
    for (i in MinUI downTo MaxUI) {
        xs.add(i)
        if (xs.size > 23) break
    }
    if (xs.size > 0) {
        throw AssertionError("Wrong elements for MaxUI..MinUI: $xs")
    }
}

fun testMaxULdownToMinUL() {
    val xs = ArrayList<ULong>()
    for (i in MinUL downTo MaxUL) {
        xs.add(i)
        if (xs.size > 23) break
    }
    if (xs.size > 0) {
        throw AssertionError("Wrong elements for MaxUI..MinUI: $xs")
    }
}

fun testWrappingULongLoop() {
    val MA = M - 1UL
    val MB = M + 1UL
    val xs = ArrayList<ULong>()
    for (i in MB downTo MA) {
        xs.add(i)
        if (xs.size > 3) break
    }
    if (xs != listOf(MB, M, MA)) throw AssertionError("$xs")
}

fun testWrappingUIntLoop() {
    val NA = N - 1u
    val NB = N + 1u
    val xs = ArrayList<UInt>()
    for (i in NB downTo NA) {
        xs.add(i)
        if (xs.size > 3) break
    }
    if (xs != listOf(NB, N, NA)) throw AssertionError("$xs")
}

fun box(): String {
    testSimpleUIntLoop()
    testEmptyUIntLoop()
    testSimpleULongLoop()
    testEmptyULongLoop()
    testULongLoop()
    testEmptyULongLoop2()
    testMaxUIdownToMinUI()
    testMaxULdownToMinUL()
    testWrappingULongLoop()
    testWrappingUIntLoop()

    return "OK"
}