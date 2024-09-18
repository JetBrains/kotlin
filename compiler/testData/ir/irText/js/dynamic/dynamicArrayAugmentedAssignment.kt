// TARGET_BACKEND: JS
// FIR_IDENTICAL
fun testArrayAugmentedAssignment(d: dynamic) {
    d["KEY"] += "+="
    d["KEY"] -= "-="
    d["KEY"] *= "*="
    d["KEY"] /= "/="
    d["KEY"] %= "%="
}
