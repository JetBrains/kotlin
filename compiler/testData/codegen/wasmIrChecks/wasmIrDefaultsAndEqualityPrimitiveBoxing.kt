// TARGET_BACKEND: WASM

// Please verify that test has at least one verification of Int boxing, owtherwise we cannot ensure that boxing check is still works

// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxInt inFunction=check1
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxInt inFunction=check2
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxInt inFunction=check3
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled=getOrBoxInt inFunction=check4
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=InlineClass1.equals inFunction=check1
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=struct.new inFunction=check1$default
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=InlineClass2.equals inFunction=check2
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=struct.new inFunction=check2$default
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=InlineClass3.equals inFunction=check3
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=struct.new inFunction=check3$default

// Primitive parameters with defaults stay unboxed, both in the stub and at a call site that passes some of them.
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxInt inFunction=primitives$default
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxLong inFunction=primitives$default
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxChar inFunction=primitives$default
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxBoolean inFunction=primitives$default
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxByte inFunction=primitives$default
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxShort inFunction=primitives$default
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxInt inFunction=callPrimitivesPartially
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxLong inFunction=callPrimitivesPartially
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxChar inFunction=callPrimitivesPartially
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxBoolean inFunction=callPrimitivesPartially
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxByte inFunction=callPrimitivesPartially
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxShort inFunction=callPrimitivesPartially
// Float and Double are not cached, so their boxes are a plain `struct.new`.
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=struct.new inFunction=floatingPoint$default
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=struct.new inFunction=callFloatingPointPartially
// A nullable primitive still needs `null` as the "not passed" value, so its default is boxed.
// These positive checks make sure the boxing function names used in the negative checks above are the right ones.
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled=getOrBoxInt inFunction=nullablePrimitive$default
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled=getOrBoxLong inFunction=nullableLong$default
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled=getOrBoxChar inFunction=nullableChar$default
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled=getOrBoxBoolean inFunction=nullableBoolean$default
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled=getOrBoxByte inFunction=nullableByte$default
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled=getOrBoxShort inFunction=nullableShort$default
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=nullableFloat$default
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=nullableDouble$default
// Functions with a single primitive parameter that has a default.
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxInt inFunction=intWithDefault$default
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxBoolean inFunction=booleanWithDefault$default
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=getOrBoxChar inFunction=charWithDefault$default
// A value class over a non-primitive type keeps `null` as the "not passed" value, so its default is still boxed.
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=valueClassOverString$default

value class InlineClass1(val value: Int)

value class InlineClass2(val value: InlineClass1)

value class InlineClass3<T : Int>(val value: T)

value class InlineClass4<T : Int?>(val value: T)

value class StringClass(val value: String)

fun check1(value1: InlineClass1 = InlineClass1(42)): Boolean {
    val value2 = InlineClass1(42)
    return value1 == value2
}

fun check2(value1: InlineClass2 = InlineClass2(InlineClass1(42))): Boolean {
    val value2 = InlineClass2(InlineClass1(42))
    return value1 == value2
}

fun check3(value1: InlineClass3<Int> = InlineClass3(42)): Boolean {
    val value2 = InlineClass3(42)
    return value1 == value2
}

fun check4(value1: InlineClass4<Int> = InlineClass4(42)): Boolean {
    val value2 = InlineClass4(42)
    return value1 == value2
}

fun primitives(i: Int = 1, l: Long = 2L, c: Char = 'c', z: Boolean = true, b: Byte = 5, s: Short = 6): Long =
    i + l + c.code + (if (z) 1 else 0) + b + s

fun callPrimitivesPartially(i: Int, z: Boolean): Long = primitives(i, z = z)

fun floatingPoint(f: Float = 1.5f, d: Double = 2.5): Double = f + d

fun callFloatingPointPartially(d: Double): Double = floatingPoint(d = d)

fun nullablePrimitive(x: Int? = 5): Int? = x

fun nullableLong(x: Long? = 5L): Long? = x

fun nullableChar(x: Char? = 'n'): Char? = x

fun nullableBoolean(x: Boolean? = true): Boolean? = x

fun nullableByte(x: Byte? = 5): Byte? = x

fun nullableShort(x: Short? = 5): Short? = x

fun nullableFloat(x: Float? = 5.5f): Float? = x

fun nullableDouble(x: Double? = 5.5): Double? = x

fun intWithDefault(x: Int = 7): Int = x

fun booleanWithDefault(x: Boolean = true): Boolean = x

fun charWithDefault(x: Char = 'q'): Char = x

fun valueClassOverString(value: StringClass = StringClass("default")): String = value.value

fun box(): String {
    if (!check1()) return "FAIL1"
    if (!check2()) return "FAIL2"
    if (!check3()) return "FAIL3"
    if (!check4()) return "FAIL4"

    if (primitives() != 114L) return "FAIL5: ${primitives()}"
    if (callPrimitivesPartially(0, false) != 112L) return "FAIL6: ${callPrimitivesPartially(0, false)}"
    if (primitives(i = 10, c = 'z', s = 0) != 140L) return "FAIL7: ${primitives(i = 10, c = 'z', s = 0)}"
    if (floatingPoint() != 4.0) return "FAIL8: ${floatingPoint()}"
    if (callFloatingPointPartially(0.0) != 1.5) return "FAIL9: ${callFloatingPointPartially(0.0)}"

    // An explicitly passed value equal to the placeholder of an omitted one must not be replaced by the default.
    if (intWithDefault(0) != 0) return "FAIL10"
    if (intWithDefault() != 7) return "FAIL11"
    if (booleanWithDefault(false)) return "FAIL12"
    if (!booleanWithDefault()) return "FAIL13"
    if (charWithDefault('\u0000') != '\u0000') return "FAIL14"
    if (charWithDefault() != 'q') return "FAIL15"
    if (floatingPoint(0.0f, 0.0) != 0.0) return "FAIL16"

    if (nullablePrimitive(null) != null) return "FAIL17"
    if (nullablePrimitive() != 5) return "FAIL18"
    if (nullablePrimitive(0) != 0) return "FAIL19"

    if (nullableLong() != 5L || nullableLong(null) != null) return "FAIL22"
    if (nullableChar() != 'n' || nullableChar(null) != null) return "FAIL23"
    if (nullableBoolean() != true || nullableBoolean(null) != null) return "FAIL24"
    if (nullableByte() != 5.toByte() || nullableByte(null) != null) return "FAIL25"
    if (nullableShort() != 5.toShort() || nullableShort(null) != null) return "FAIL26"
    if (nullableFloat() != 5.5f || nullableFloat(null) != null) return "FAIL27"
    if (nullableDouble() != 5.5 || nullableDouble(null) != null) return "FAIL28"

    if (valueClassOverString() != "default") return "FAIL20"
    if (valueClassOverString(StringClass("")) != "") return "FAIL21"

    return "OK"
}
