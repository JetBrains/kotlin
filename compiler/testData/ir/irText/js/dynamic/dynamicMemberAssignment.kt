// TARGET_BACKEND: JS_IR
fun testMemberAssignment(d: dynamic) {
    d.m = 1
}

fun testSafeMemberAssignment(d: dynamic) {
    d?.m = 1
}
