// TARGET_BACKEND: JS_IR
fun testArrayAssignment(d: dynamic) {
    d["KEY"] = 1
}

fun testArrayAssignmentFake(d: dynamic) {
    d.set("KEY", 2)
}
