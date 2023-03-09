// TARGET_BACKEND: JS_IR

// NO_SIGNATURE_DUMP
// ^ KT-57566

fun testMemberAssignment(d: dynamic) {
    d.m = 1
}

fun testSafeMemberAssignment(d: dynamic) {
    d?.m = 1
}
