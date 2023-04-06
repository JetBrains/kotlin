// TARGET_BACKEND: JS_IR
// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57566

fun testArrayAugmentedAssignment(d: dynamic) {
    d["KEY"] += "+="
    d["KEY"] -= "-="
    d["KEY"] *= "*="
    d["KEY"] /= "/="
    d["KEY"] %= "%="
}
