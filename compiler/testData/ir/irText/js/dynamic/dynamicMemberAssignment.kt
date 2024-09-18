// TARGET_BACKEND: JS
fun testMemberAssignment(d: dynamic) {
    d.m = 1
}

fun testSafeMemberAssignment(d: dynamic) {
    d?.m = 1
}
