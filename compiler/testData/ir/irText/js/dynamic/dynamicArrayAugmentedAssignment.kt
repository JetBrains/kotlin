// TARGET_BACKEND: JS_IR

fun testArrayAugmentedAssignment(d: dynamic) {
    d["KEY"] += "+="
    d["KEY"] -= "-="
    d["KEY"] *= "*="
    d["KEY"] /= "/="
    d["KEY"] %= "%="
}
