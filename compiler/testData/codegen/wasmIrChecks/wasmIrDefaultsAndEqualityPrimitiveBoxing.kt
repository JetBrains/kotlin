// TARGET_BACKEND: WASM

// FUNCTION_CONTAINS_NO_CALLS: shouldBeCalled=getOrBoxInt inFunction=check1
// FUNCTION_CONTAINS_NO_CALLS: shouldBeCalled=getOrBoxInt inFunction=check2
// FUNCTION_CONTAINS_NO_CALLS: shouldBeCalled=getOrBoxInt inFunction=check3
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled=getOrBoxInt inFunction=check4 count=1

value class InlineClass1(val value: Int)

value class InlineClass2(val value: InlineClass1)

value class InlineClass3<T : Int>(val value: T)

value class InlineClass4<T : Int?>(val value: T)

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

fun box(): String {
    if (!check1()) return "FAIL1"
    if (!check2()) return "FAIL2"
    if (!check3()) return "FAIL3"
    if (!check4()) return "FAIL4"
    return "OK"
}
