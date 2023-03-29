// TARGET_BACKEND: JS_IR

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57566

fun testMemberAssignment(d: dynamic) {
    d.m = 1
}

fun testSafeMemberAssignment(d: dynamic) {
    d?.m = 1
}
