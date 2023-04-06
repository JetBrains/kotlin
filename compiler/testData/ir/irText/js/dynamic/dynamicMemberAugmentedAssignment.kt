// TARGET_BACKEND: JS_IR

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57566

fun testAugmentedMemberAssignment(d: dynamic) {
    d.m += "+="
    d.m -= "-="
    d.m *= "*="
    d.m /= "/="
    d.m %= "%="
}

// see KT-29768
fun testSafeAugmentedMemberAssignment(d: dynamic) {
    d?.m += "+="
    d?.m -= "-="
    d?.m *= "*="
    d?.m /= "/="
    d?.m %= "%="
}
