// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

const val MaxUI = UInt.MAX_VALUE
const val MinUI = UInt.MIN_VALUE

const val MaxUL = ULong.MAX_VALUE
const val MinUL = ULong.MIN_VALUE

val M = MaxUI.toULong()

val p1 = 6u downTo 1u
fun testSimpleUIntLoop() {
    var s = 0
    for (i in p1) {
        s = s*10 + i.toInt()
    }
    if (s != 654321) throw AssertionError("$s")
}

val p2 = 1u downTo 6u
fun testEmptyUIntLoop() {
    var s = 0
    for (i in p2) {
        s = s*10 + i.toInt()
    }
    if (s != 0) throw AssertionError("$s")
}

val p3 = 6UL downTo 1UL
fun testSimpleULongLoop() {
    var s = 0
    for (i in p3) {
        s = s*10 + i.toInt()
    }
    if (s != 654321) throw AssertionError("$s")
}

val p4 = 1UL downTo 6UL
fun testEmptyULongLoop() {
    var s = 0
    for (i in p4) {
        s = s*10 + i.toInt()
    }
    if (s != 0) throw AssertionError("$s")
}

val p5 = M + 6UL downTo M + 1UL
fun testULongLoop() {
    var s = 0
    for (i in p5) {
        s = s*10 + (i-M).toInt()
    }
    if (s != 654321) throw AssertionError("$s")
}

val p6 = M + 1UL downTo M + 6UL
fun testEmptyULongLoop2() {
    var s = 0
    for (i in p6) {
        s = s*10 + (i-M).toInt()
    }
    if (s != 0) throw AssertionError("$s")
}

val p7 = MinUI downTo MaxUI
fun testMaxUIdownToMinUI() {
    val xs = ArrayList<UInt>()
    for (i in p7) {
        xs.add(i)
        if (xs.size > 23) break
    }
    if (xs.size > 0) {
        throw AssertionError("Wrong elements for MaxUI..MinUI: $xs")
    }
}

val p8 = MinUL downTo MaxUL
fun testMaxULdownToMinUL() {
    val xs = ArrayList<ULong>()
    for (i in p8) {
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
    testMaxUIdownToMinUI()
    testMaxULdownToMinUL()

    return "OK"
}