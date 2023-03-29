// TARGET_BACKEND: JS_IR

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57566

fun testArrayAssignment(d: dynamic) {
    d["KEY"] = 1
}

fun testArrayAssignmentFake(d: dynamic) {
    d.set("KEY", 2)
}
