// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNSIGNED_ARRAYS
// !LANGUAGE: +InlineClasses
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun box(): String {
    testForInUIntArrayWithUpcactToAny()
    testForInUIntArrayWithUpcactToComparable()

    return "OK"
}

fun testForInUIntArrayWithUpcactToAny() {
    var test = ""
    for (x: Any in uintArrayOf(1u, 2u, 3u)) {
        test = "$test$x;"
        useUIntAsAny(x)
    }
    if (test != "1;2;3;") throw AssertionError(test)
}

fun testForInUIntArrayWithUpcactToComparable() {
    var test = ""
    for (x: Comparable<*> in uintArrayOf(1u, 2u, 3u)) {
        test = "$test$x;"
        useUIntAsComparable(x)
    }
    if (test != "1;2;3;") throw AssertionError(test)
}

fun useUIntAsAny(a: Any) {
    a as UInt
}

fun useUIntAsComparable(a: Comparable<*>) {
    a as Comparable<*>
}