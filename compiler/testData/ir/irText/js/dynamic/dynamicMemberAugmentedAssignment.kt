// TARGET_BACKEND: JS_IR

// NO_SIGNATURE_DUMP
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
