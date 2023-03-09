// TARGET_BACKEND: JS_IR

// NO_SIGNATURE_DUMP
// ^ KT-57566

fun testArrayAssignment(d: dynamic) {
    d["KEY"] = 1
}

fun testArrayAssignmentFake(d: dynamic) {
    d.set("KEY", 2)
}
