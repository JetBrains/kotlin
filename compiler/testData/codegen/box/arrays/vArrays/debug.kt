// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER

fun isBoolVArray(p: Any?) = p is VArray<Boolean>
fun isByteVArray(p: Any?) = p is VArray<Byte>
fun isShortVArray(p: Any?) = p is VArray<Short>
fun isIntVArray(p: Any?) = p is VArray<Int>
fun isLongVArray(p: Any?) = p is VArray<Long>
fun isFloatVArray(p: Any?) = p is VArray<Float>
fun isDoubleVArray(p: Any?) = p is VArray<Double>
fun isCharVArray(p: Any?) = p is VArray<Char>
fun isICVArray(p: Any?) = p is VArray<IC>

@JvmInline
value class IC(val x: Int)


fun box(): String {

    val boolVArray: Any = VArray<Boolean>(1) { true }

    if (!isBoolVArray(boolVArray)) return "Fail 1.1"
    if (isByteVArray(boolVArray)) return "Fail 1.2"
    if (isShortVArray(boolVArray)) return "Fail 1.3"
    if (isIntVArray(boolVArray)) return "Fail 1.4"
    if (isLongVArray(boolVArray)) return "Fail 1.5"
    if (isFloatVArray(boolVArray)) return "Fail 1.6"
    if (isDoubleVArray(boolVArray)) return "Fail 1.7"
    if (isCharVArray(boolVArray)) return "Fail 1.8"
    if (isICVArray(boolVArray)) return "Fail 1.9"

    val icVArray: Any = VArray<IC>(1) { IC(42) }

    if (isBoolVArray(icVArray)) return "Fail 2.1"
    if (isByteVArray(icVArray)) return "Fail 2.2"
    if (isShortVArray(icVArray)) return "Fail 2.3"
    if (isIntVArray(icVArray)) return "Fail 2.4"
    if (isLongVArray(icVArray)) return "Fail 2.5"
    if (isFloatVArray(icVArray)) return "Fail 2.6"
    if (isDoubleVArray(icVArray)) return "Fail 2.7"
    if (isCharVArray(icVArray)) return "Fail 2.8"
    if (!isICVArray(icVArray)) return "Fail 2.9"

    return "OK"
}

