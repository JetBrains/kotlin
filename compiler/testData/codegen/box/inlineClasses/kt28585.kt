// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// !LANGUAGE: +InlineClasses
// WITH_RUNTIME

fun box(): String {
    var uint1 = 1u
    var uint2 = 2u
    var uint3 = 3u
    val uintSet = mutableSetOf(uint1)
    uintSet.add(uint2);
    {
        uintSet.add(uint3)
        if (!uintSet.contains(1u)) throw AssertionError()
        if (!uintSet.contains(2u)) throw AssertionError()
        if (!uintSet.contains(3u)) throw AssertionError()
    }()
    return "OK"
}