// WITH_STDLIB

val UB_MAX = UByte.MAX_VALUE
val UB_START = (UB_MAX - 10u).toUByte()

val US_MAX = UShort.MAX_VALUE
val US_START = (US_MAX - 10u).toUShort()

fun testUByteLoopWithCoercion1() {
    for (x in UB_START..UB_MAX) {
        if (x > UB_MAX.toUInt()) throw AssertionError()
    }
}

fun testUByteLoopWithCoercion2() {
    for (x in UB_START until UB_MAX) {
        if (x > UB_MAX.toUInt()) throw AssertionError()
    }
}

fun testUByteLoopWithCoercion3() {
    for (x in UB_MAX downTo UB_START) {
        if (x > UB_MAX.toUInt()) throw AssertionError()
    }
}


fun testUShortLoopWithCoercion1() {
    for (x in US_START..US_MAX) {
        if (x > US_MAX.toUInt()) throw AssertionError()
    }
}

fun testUShortLoopWithCoercion2() {
    for (x in US_START until US_MAX) {
        if (x > US_MAX.toUInt()) throw AssertionError()
    }
}

fun testUShortLoopWithCoercion3() {
    for (x in US_MAX downTo US_START) {
        if (x > US_MAX.toUInt()) throw AssertionError()
    }
}


fun box(): String {
    testUByteLoopWithCoercion1()
    testUByteLoopWithCoercion2()
    testUByteLoopWithCoercion3()
    testUShortLoopWithCoercion1()
    testUShortLoopWithCoercion2()
    testUShortLoopWithCoercion3()

    return "OK"
}