// WITH_UNSIGNED
// IGNORE_BACKEND: JS_IR, JVM_IR, JS

fun prefixDecrementUByteLocal(): Any? {
    var a: UByte = 0u
    return --a
}

fun prefixDecrementUShortLocal(): Any? {
    var a: UShort = 0u
    return --a
}

fun prefixDecrementUIntLocal(): Any? {
    var a: UInt = 0u
    return --a
}

fun prefixDecrementULongLocal(): Any? {
    var a: ULong = 0u
    return --a
}

fun prefixIncrementUByteLocal(): Any? {
    var a: UByte = 0u
    return ++a
}

fun prefixIncrementUShortLocal(): Any? {
    var a: UShort = 0u
    return ++a
}

fun prefixIncrementUIntLocal(): Any? {
    var a: UInt = 0u
    return ++a
}

fun prefixIncrementULongLocal(): Any? {
    var a: ULong = 0u
    return ++a
}

var gb: UByte = 0u
var gs: UShort = 0u
var gi: UInt = 0u
var gl: ULong = 0UL

fun prefixDecrementUByteProperty(): Any? {
    gb = 0u
    return --gb
}

fun prefixDecrementUShortProperty(): Any? {
    gs = 0u
    return --gs
}

fun prefixDecrementUIntProperty(): Any? {
    gi = 0u
    return --gi
}

fun prefixDecrementULongProperty(): Any? {
    gl = 0u
    return --gl
}

fun prefixIncrementUByteProperty(): Any? {
    gb = 0u
    return ++gb
}

fun prefixIncrementUShortProperty(): Any? {
    gs = 0u
    return ++gs
}

fun prefixIncrementUIntProperty(): Any? {
    gi = 0u
    return ++gi
}

fun prefixIncrementULongProperty(): Any? {
    gl = 0u
    return ++gl
}

fun check(u: Any?, ts: String, className: String) {
    u!!
    if (u.toString() != ts) throw AssertionError(u.toString())
    if (u.javaClass.name != className) throw AssertionError(u.javaClass.name)
}

fun box(): String {
    check(prefixDecrementUByteLocal(), 0xFFu.toString(), "kotlin.UByte")
    check(prefixDecrementUShortLocal(), 0xFFFFu.toString(), "kotlin.UShort")
    check(prefixDecrementUIntLocal(), 0xFFFF_FFFFu.toString(), "kotlin.UInt")
    check(prefixDecrementULongLocal(), 0xFFFF_FFFF_FFFF_FFFFUL.toString(), "kotlin.ULong")

    check(prefixIncrementUByteLocal(), "1", "kotlin.UByte")
    check(prefixIncrementUShortLocal(), "1", "kotlin.UShort")
    check(prefixIncrementUIntLocal(), "1", "kotlin.UInt")
    check(prefixIncrementULongLocal(), "1", "kotlin.ULong")

    check(prefixDecrementUByteProperty(), 0xFFu.toString(), "kotlin.UByte")
    check(prefixDecrementUShortProperty(), 0xFFFFu.toString(), "kotlin.UShort")
    check(prefixDecrementUIntProperty(), 0xFFFF_FFFFu.toString(), "kotlin.UInt")
    check(prefixDecrementULongProperty(), 0xFFFF_FFFF_FFFF_FFFFUL.toString(), "kotlin.ULong")

    check(prefixIncrementUByteProperty(), "1", "kotlin.UByte")
    check(prefixIncrementUShortProperty(), "1", "kotlin.UShort")
    check(prefixIncrementUIntProperty(), "1", "kotlin.UInt")
    check(prefixIncrementULongProperty(), "1", "kotlin.ULong")

    return "OK"
}