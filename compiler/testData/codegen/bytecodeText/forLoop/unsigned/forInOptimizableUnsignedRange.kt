// WITH_RUNTIME

fun testUIntRangeLiteral(a: UInt, b: UInt): Int {
    var s = 0
    for (x in a .. b) {
        s += x.toInt()
    }
    return s
}

fun testULongRangeLiteral(a: ULong, b: ULong): Int {
    var s = 0
    for (x in a .. b) {
        s += x.toInt()
    }
    return s
}

fun testUIntUntil(a: UInt, b: UInt): Int {
    var s = 0
    for (x in a until b) {
        s += x.toInt()
    }
    return s
}

fun testULongUntil(a: ULong, b: ULong): Int {
    var s = 0
    for (x in a until b) {
        s += x.toInt()
    }
    return s
}

fun testUIntDownTo(a: UInt, b: UInt): Int {
    var s = 0
    for (x in a downTo b) {
        s += x.toInt()
    }
    return s
}

fun testULongDownTo(a: ULong, b: ULong): Int {
    var s = 0
    for (x in a downTo b) {
        s += x.toInt()
    }
    return s
}

// 0 iterator
// 0 hasNext
// 0 next
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 0 INVOKESTATIC kotlin/U(Int|Long).constructor-impl
// 0 INVOKE\w+ kotlin/U(Int|Long).(un)?box-impl
